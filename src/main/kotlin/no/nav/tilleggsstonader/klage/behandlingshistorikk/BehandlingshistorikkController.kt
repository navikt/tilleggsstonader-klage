package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.behandlingshistorikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.klage.behandlingshistorikk.dto.tilDto
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.klage.kabal.domain.tilDto
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandlingshistorikk"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingshistorikkController(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikk(
        @PathVariable behandlingId: BehandlingId,
    ): List<BehandlingshistorikkDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return behandlingshistorikkService.hentBehandlingshistorikk(behandlingId).tilDto()
    }
}
