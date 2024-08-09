package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.tilleggsstonader.klage.infrastruktur.config.getValue
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
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

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        return oppgaveClient.opprettOppgave(opprettOppgaveRequest)
    }

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        // Skal ikke oppdatere tema for oppgaver som alt er ferdigstilt
        if (!behandling.status.erUnderArbeidAvSaksbehandler()) return

        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        // TODO: Bør sende med oppgaveId til EksternBehandlingContoller og deretter slette dette kallet
        val oppgave = hentOppgave(behandleSakOppgave.oppgaveId)

        val oppdatertOppgave = Oppgave(
            id = behandleSakOppgave.oppgaveId,
            behandlingstema = Behandlingstema.Tilbakebetaling.value,
            versjon = oppgave.versjon,
        )

        oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }

    fun hentOppgave(gsakOppgaveId: Long): Oppgave {
        return oppgaveClient.finnOppgaveMedId(gsakOppgaveId)
    }

    fun finnMappe(enhet: String, oppgaveMappe: OppgaveMappe) = finnMapper(enhet)
        .filter { it.navn.endsWith(oppgaveMappe.navn, ignoreCase = true) }
        .let {
            if (it.size != 1) {
                secureLogger.error("Finner ${it.size} mapper for enhet=$enhet navn=$oppgaveMappe - mapper=$it")
                error("Finner ikke mapper for enhet=$enhet navn=$oppgaveMappe. Se secure logs for mer info")
            }
            it.single()
        }
        .id.toLong()

    fun finnMapper(enheter: List<String>): List<MappeDto> {
        return enheter.flatMap { finnMapper(it) }
    }

    fun finnMapper(enhet: String): List<MappeDto> {
        return cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons = oppgaveClient.finnMapper(
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
    }
}
