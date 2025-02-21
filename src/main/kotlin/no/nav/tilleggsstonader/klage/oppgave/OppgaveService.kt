package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.spring.cache.getValue
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
    private val oppgaveClient: OppgaveClient,
    private val behandlingService: BehandlingService,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgaveUtenÅLagreIRepository(opprettOppgaveRequest: OpprettOppgaveRequest): Long =
        oppgaveClient.opprettOppgave(opprettOppgaveRequest)

    fun opprettOppgave(
        behandlingId: BehandlingId,
        opprettOppgaveRequest: OpprettOppgaveRequest,
    ): Long {
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        behandleSakOppgaveRepository.insert(BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId))
        return oppgaveId
    }

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        // Skal ikke oppdatere tema for oppgaver som alt er ferdigstilt
        if (!behandling.status.erUnderArbeidAvSaksbehandler()) return

        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        // TODO: Bør sende med oppgaveId til EksternBehandlingContoller og deretter slette dette kallet
        val oppgave = hentOppgave(behandleSakOppgave.oppgaveId)

        val oppdatertOppgave =
            Oppgave(
                id = behandleSakOppgave.oppgaveId,
                behandlingstema = Behandlingstema.Tilbakebetaling.value,
                versjon = oppgave.versjon,
            )

        oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }

    fun hentOppgave(gsakOppgaveId: Long): Oppgave = oppgaveClient.finnOppgaveMedId(gsakOppgaveId)

    fun hentBehandlingIderForOppgaver(oppgaveIder: List<Long>): Map<Long, UUID> =
        behandleSakOppgaveRepository
            .finnForOppgaveIder(oppgaveIder)
            .associate { it.oppgaveId to it.behandlingId.id }

    fun hentOppgaveIdForBehandling(behandlingId: BehandlingId): BehandleSakOppgave =
        behandleSakOppgaveRepository.findByBehandlingId(behandlingId)

    fun finnMappe(
        enhet: String,
        oppgaveMappe: OppgaveMappe,
    ) = finnMapper(enhet)
        .let { alleMapper ->
            val aktuelleMapper =
                alleMapper.filter { mappe ->
                    oppgaveMappe.navn.any { mappe.navn.endsWith(it, ignoreCase = true) }
                }
            if (aktuelleMapper.size != 1) {
                secureLogger.error("Finner ${aktuelleMapper.size} mapper for enhet=$enhet navn=$oppgaveMappe - mapper=$alleMapper")
                error("Finner ikke mapper for enhet=$enhet navn=$oppgaveMappe. Se secure logs for mer info")
            }
            aktuelleMapper.single()
        }

    fun finnMapper(enhet: String): List<MappeDto> =
        cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons =
                oppgaveClient.finnMapper(
                    enhetsnummer = enhet,
                    limit = 1000,
                )
            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error(
                    "Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                        "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. ",
                )
            }
            mappeRespons.mapper
        }

    fun settPåVent(settPåVent: SettPåVentRequest): SettPåVentResponse = oppgaveClient.settPåVent(settPåVent)

    fun oppdaterPåVent(oppdaterPåVent: OppdaterPåVentRequest): SettPåVentResponse = oppgaveClient.oppdaterPåVent(oppdaterPåVent)

    fun taAvVent(taAvVent: TaAvVentRequest): SettPåVentResponse = oppgaveClient.taAvVent(taAvVent)
}
