package no.nav.tilleggsstonader.klage.kabal.event

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.kabal.BehandlingFeilregistrertTask
import no.nav.tilleggsstonader.klage.kabal.KabalBehandlingEvent
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KabalBehandlingEventService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val klageresultatRepository: KlageresultatRepository,
    private val stegService: StegService,
    private val integrasjonerClient: TilleggsstønaderIntegrasjonerClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleEvent(kabalBehandlingEvent: KabalBehandlingEvent) {
        val finnesKlageresultat = klageresultatRepository.existsById(kabalBehandlingEvent.eventId)
        if (finnesKlageresultat) {
            logger.warn("Hendelse fra kabal med eventId: ${kabalBehandlingEvent.eventId} er allerede lest - prosesserer ikke hendelse.")
        } else {
            logger.info("Prosesserer hendelse fra kabal med eventId: ${kabalBehandlingEvent.eventId}")
            val eksternBehandlingId = UUID.fromString(kabalBehandlingEvent.kildeReferanse)
            val behandling = behandlingRepository.findByEksternBehandlingId(eksternBehandlingId)

            lagreKlageresultat(kabalBehandlingEvent, behandling)

            when (kabalBehandlingEvent.type) {
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> behandleKlageAvsluttet(behandling, kabalBehandlingEvent)
                BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
                BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET,
                -> opprettOppgaveTask(behandling, kabalBehandlingEvent)

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

    private fun opprettBehandlingFeilregistretTask(behandlingId: BehandlingId) {
        taskService.save(BehandlingFeilregistrertTask.opprettTask(behandlingId))
    }

    private fun lagreKlageresultat(
        kabalBehandlingEvent: KabalBehandlingEvent,
        behandling: Behandling,
    ) {
        val klageinstansResultat =
            KlageinstansResultat(
                eventId = kabalBehandlingEvent.eventId,
                type = kabalBehandlingEvent.type,
                utfall = kabalBehandlingEvent.utfall(),
                mottattEllerAvsluttetTidspunkt = kabalBehandlingEvent.mottattEllerAvsluttetTidspunkt(),
                kildereferanse = UUID.fromString(kabalBehandlingEvent.kildeReferanse),
                journalpostReferanser = StringListWrapper(kabalBehandlingEvent.journalpostReferanser()),
                behandlingId = behandling.id,
                årsakFeilregistrert = utledÅrsakFeilregistrert(kabalBehandlingEvent),
            )

        klageresultatRepository.insert(klageinstansResultat)
    }

    private fun utledÅrsakFeilregistrert(kabalBehandlingEvent: KabalBehandlingEvent) =
        if (kabalBehandlingEvent.type == BehandlingEventType.BEHANDLING_FEILREGISTRERT) {
            kabalBehandlingEvent.detaljer.behandlingFeilregistrert?.reason
                ?: error("Finner ikke årsak til feilregistrering")
        } else {
            null
        }

    private fun behandleKlageAvsluttet(
        behandling: Behandling,
        kabalBehandlingEvent: KabalBehandlingEvent,
    ) {
        when (behandling.status) {
            BehandlingStatus.FERDIGSTILT ->
                logger.error(
                    "Mottatt event på ferdigstilt behandling $kabalBehandlingEvent - event kan være lest fra før",
                )
            else -> {
                opprettOppgaveTask(behandling, kabalBehandlingEvent)
                ferdigstillKlagebehandling(behandling)
            }
        }
    }

    private fun opprettOppgaveTask(
        behandling: Behandling,
        kabalBehandlingEvent: KabalBehandlingEvent,
    ) {
        val fagsakDomain =
            fagsakRepository.finnFagsakForBehandlingId(behandling.id)
                ?: error("Finner ikke fagsak for behandlingId: ${behandling.id}")
        val saksbehandlerIdent = behandling.sporbar.endret.endretAv
        val saksbehandlerEnhet = utledSaksbehandlerEnhet(saksbehandlerIdent)
        val oppgaveTekst =
            "${kabalBehandlingEvent.detaljer.oppgaveTekst(saksbehandlerEnhet)} Gjelder: ${fagsakDomain.stønadstype}"
        val klageBehandlingEksternId = UUID.fromString(kabalBehandlingEvent.kildeReferanse)
        val opprettOppgavePayload =
            OpprettOppgavePayload(
                klagebehandlingEksternId = klageBehandlingEksternId,
                oppgaveTekst = oppgaveTekst,
                fagsystem = fagsakDomain.fagsystem,
                klageinstansUtfall = kabalBehandlingEvent.utfall(),
                behandlingstema = fagsakDomain.stønadstype.tilBehandlingstema(),
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

    private fun ferdigstillKlagebehandling(behandling: Behandling) {
        stegService.oppdaterSteg(behandling.id, StegType.KABAL_VENTER_SVAR, StegType.BEHANDLING_FERDIGSTILT)
    }
}
