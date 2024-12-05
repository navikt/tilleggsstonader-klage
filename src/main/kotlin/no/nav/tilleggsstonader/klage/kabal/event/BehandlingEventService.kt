package no.nav.tilleggsstonader.klage.kabal.event

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.kabal.BehandlingEvent
import no.nav.tilleggsstonader.klage.kabal.BehandlingFeilregistrertTask
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingEventService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val klageresultatRepository: KlageresultatRepository,
    private val stegService: StegService,
    private val integrasjonerClient: TilleggsstønaderIntegrasjonerClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleEvent(behandlingEvent: BehandlingEvent) {
        val finnesKlageresultat = klageresultatRepository.existsById(behandlingEvent.eventId)
        if (finnesKlageresultat) {
            logger.warn("Hendelse fra kabal med eventId: ${behandlingEvent.eventId} er allerede lest - prosesserer ikke hendelse.")
        } else {
            logger.info("Prosesserer hendelse fra kabal med eventId: ${behandlingEvent.eventId}")
            val eksternBehandlingId = UUID.fromString(behandlingEvent.kildeReferanse)
            val behandling = behandlingRepository.findByEksternBehandlingId(eksternBehandlingId)

            lagreKlageresultat(behandlingEvent, behandling)

            when (behandlingEvent.type) {
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> behandleKlageAvsluttet(behandling, behandlingEvent)
                BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
                BehandlingEventType.OMGJOERINGSKRAV_AVSLUTTET,
                -> opprettOppgaveTask(behandling, behandlingEvent)

                BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
                BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
                -> {
                    /*
                     * Skal ikke gjøre noe dersom eventtype er ANKEBEHANDLING_OPPRETTET
                     * eller ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET
                     * */
                }

                BehandlingEventType.BEHANDLING_FEILREGISTRERT -> opprettBehandlingFeilregistretTask(behandling.id)
            }
        }
    }

    private fun opprettBehandlingFeilregistretTask(behandlingId: UUID) {
        taskService.save(BehandlingFeilregistrertTask.opprettTask(behandlingId))
    }

    private fun lagreKlageresultat(behandlingEvent: BehandlingEvent, behandling: Behandling) {
        val klageinstansResultat = KlageinstansResultat(
            eventId = behandlingEvent.eventId,
            type = behandlingEvent.type,
            utfall = behandlingEvent.utfall(),
            mottattEllerAvsluttetTidspunkt = behandlingEvent.mottattEllerAvsluttetTidspunkt(),
            kildereferanse = UUID.fromString(behandlingEvent.kildeReferanse),
            journalpostReferanser = StringListWrapper(behandlingEvent.journalpostReferanser()),
            behandlingId = behandling.id,
            årsakFeilregistrert = utledÅrsakFeilregistrert(behandlingEvent),
        )

        klageresultatRepository.insert(klageinstansResultat)
    }

    private fun utledÅrsakFeilregistrert(behandlingEvent: BehandlingEvent) =
        if (behandlingEvent.type == BehandlingEventType.BEHANDLING_FEILREGISTRERT) {
            behandlingEvent.detaljer.behandlingFeilregistrert?.reason
                ?: error("Finner ikke årsak til feilregistrering")
        } else {
            null
        }

    private fun behandleKlageAvsluttet(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        when (behandling.status) {
            BehandlingStatus.FERDIGSTILT -> logger.error("Mottatt event på ferdigstilt behandling $behandlingEvent - event kan være lest fra før")
            else -> {
                opprettOppgaveTask(behandling, behandlingEvent)
                ferdigstillKlagebehandling(behandling)
            }
        }
    }

    private fun opprettOppgaveTask(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
            ?: error("Finner ikke fagsak for behandlingId: ${behandling.id}")
        val saksbehandlerIdent = behandling.sporbar.endret.endretAv
        val saksbehandlerEnhet = utledSaksbehandlerEnhet(saksbehandlerIdent)
        val oppgaveTekst =
            "${behandlingEvent.detaljer.oppgaveTekst(saksbehandlerEnhet)} Gjelder: ${fagsakDomain.stønadstype}"
        val klageBehandlingEksternId = UUID.fromString(behandlingEvent.kildeReferanse)
        val opprettOppgavePayload = OpprettOppgavePayload(
            klagebehandlingEksternId = klageBehandlingEksternId,
            oppgaveTekst = oppgaveTekst,
            fagsystem = fagsakDomain.fagsystem,
            klageinstansUtfall = behandlingEvent.utfall(),
            behandlingstema = finnBehandlingstema(fagsakDomain.stønadstype),
        )
        val opprettOppgaveTask = OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload)
        taskService.save(opprettOppgaveTask)
    }

    private fun utledSaksbehandlerEnhet(saksbehandlerIdent: String) =
        try {
            integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent).enhet
        } catch (e: ProblemDetailException) {
            logger.error("Kunne ikke hente enhet for saksbehandler med ident=$saksbehandlerIdent")
            secureLogger.error("Kunne ikke hente enhet for saksbehandler med ident=$saksbehandlerIdent", e)
            "Ukjent"
        }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> Behandlingstema.TilsynBarn
            Stønadstype.LÆREMIDLER -> error("TODO: Funksjonaliteten er ikke implementert for LÆREMIDLER enda")
        }
    }

    private fun ferdigstillKlagebehandling(behandling: Behandling) {
        stegService.oppdaterSteg(behandling.id, StegType.KABAL_VENTER_SVAR, StegType.BEHANDLING_FERDIGSTILT)
    }
}
