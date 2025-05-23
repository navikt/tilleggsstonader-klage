package no.nav.tilleggsstonader.klage.ekstern

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.tilleggsstonader.klage.behandling.domain.tilEksternKlagebehandlingDto
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
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

    @GetMapping(path = ["/{fagsystem}"])
    fun finnKlagebehandlingsresultat(
        @PathVariable fagsystem: Fagsystem,
        @RequestParam("eksternFagsakId") eksternFagsakIder: Set<String>,
    ): Map<String, List<KlagebehandlingDto>> {
        tilgangService.validerHarVeilederrolleTilStønadForFagsystem(fagsystem)

        brukerfeilHvis(eksternFagsakIder.isEmpty()) {
            "Mangler eksternFagsakId i query param"
        }

        return eksternFagsakIder
            .associateWith {
                behandlingService
                    .finnKlagebehandlingsresultat(eksternFagsakId = it, fagsystem = fagsystem)
            }.also {
                loggAntallTreffPerFagsak(behandlinger = it)
                validerTilgang(behandlinger = it)
            }.mapValues { (_, behandlinger) ->
                behandlinger.map {
                    it.tilEksternKlagebehandlingDto(
                        klageinstansResultat = behandlingService.hentKlageresultatDto(behandlingId = it.id),
                    )
                }
            }
    }

    @PostMapping(path = ["finn-oppgaver"])
    fun hentBehandlingIderForOppgaver(
        @RequestBody request: OppgaverBehandlingerRequest,
    ): OppgaverBehandlingerResponse {
        val behandlingIdPåOppgaveId = oppgaveService.hentBehandlingIderForOppgaver(request.oppgaveIder)
        return OppgaverBehandlingerResponse(oppgaver = behandlingIdPåOppgaveId)
    }

    @PostMapping("/opprett")
    fun opprettBehandling(
        @RequestBody opprettKlageBehandlingDto: OpprettKlagebehandlingRequest,
    ) {
        opprettBehandlingService.opprettBehandling(opprettKlageBehandlingDto)
    }

    @PatchMapping("{behandlingId}/gjelder-tilbakekreving")
    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(
        @PathVariable behandlingId: BehandlingId,
    ) {
        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
    }

    private fun loggAntallTreffPerFagsak(behandlinger: Map<String, List<Klagebehandlingsresultat>>) {
        val antallTreffPerFagsak = behandlinger.entries.associate { it.key to it.value.size }
        logger.info("Antall klagebehandlinger funnet per fagsak: $antallTreffPerFagsak")
    }

    private fun validerTilgang(behandlinger: Map<String, List<Klagebehandlingsresultat>>) {
        behandlinger.entries.flatMap { it.value }.map { it.ident }.distinct().forEach {
            tilgangService.validerTilgangTilPerson(it, AuditLoggerEvent.ACCESS)
        }
    }
}
