package no.nav.tilleggsstonader.klage.formkrav

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.formkrav.dto.FormkravDto
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/formkrav"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FormController(
    private val formService: FormService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("vilkar/{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): FormkravDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return formService.hentFormDto(behandlingId)
    }

    @PostMapping
    fun oppdaterFormkravVilkår(
        @RequestBody form: FormkravDto,
    ): FormkravDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(form.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(form.behandlingId)
        return formService.oppdaterFormkrav(form)
    }
}
