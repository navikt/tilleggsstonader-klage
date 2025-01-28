package no.nav.tilleggsstonader.klage.ekstern

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.tilEksternKlagebehandlingDto
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.klage.oppgave.OppgaveService
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.OppgaverBehandlingerRequest
import no.nav.tilleggsstonader.kontrakter.klage.OppgaverBehandlingerResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/ekstern/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EksternBehandlingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("{fagsystem}")
    fun finnKlagebehandlingsresultat(
        @PathVariable fagsystem: Fagsystem,
        @RequestParam("eksternFagsakId") eksternFagsakIder: Set<String>,
    ): Ressurs<Map<String, List<KlagebehandlingDto>>> {
        feilHvis(eksternFagsakIder.isEmpty()) {
            "Mangler eksternFagsakId i query param"
        }
        val behandlinger =
            eksternFagsakIder.associateWith { eksternFagsakId ->
                behandlingService.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem).map {
                    it.tilEksternKlagebehandlingDto(behandlingService.hentKlageresultatDto(behandlingId = it.id))
                }
            }
        val antallTreff = behandlinger.entries.associate { it.key to it.value.size }
        logger.info("Henter klagebehandlingsresultat for eksternFagsakIder=$eksternFagsakIder antallTreff=$antallTreff")
        validerTilgang(behandlinger)

        return Ressurs.success(behandlinger)
    }

    @PostMapping("finn-oppgaver")
    fun hentBehandlingIderForOppgaver(
        @RequestBody request: OppgaverBehandlingerRequest,
    ): Ressurs<OppgaverBehandlingerResponse> {
        val behandlingIdPåOppgaveId = oppgaveService.hentBehandlingIderForOppgaver(request.oppgaveIder)
        return Ressurs.success(OppgaverBehandlingerResponse(oppgaver = behandlingIdPåOppgaveId))
    }

    private fun validerTilgang(behandlinger: Map<String, List<KlagebehandlingDto>>) {
        behandlinger.entries.flatMap { it.value }.map { it.fagsakId }.distinct().forEach {
            tilgangService.validerTilgangTilPersonMedRelasjonerForFagsak(it, AuditLoggerEvent.ACCESS)
            tilgangService.validerHarVeilederrolleTilStønadForFagsak(it)
        }
    }

    @PostMapping("/opprett")
    fun opprettBehandling(
        @RequestBody opprettKlageBehandlingDto: OpprettKlagebehandlingRequest,
    ) {
        opprettBehandlingService.opprettBehandling(opprettKlageBehandlingDto)
    }

    @PatchMapping("{behandlingId}/gjelder-tilbakekreving")
    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(
        @PathVariable behandlingId: UUID,
    ) {
        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
    }
}
