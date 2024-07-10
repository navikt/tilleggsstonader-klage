package no.nav.tilleggsstonader.klage.brev

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.brev.dto.BrevmottakereDto
import no.nav.tilleggsstonader.klage.brev.dto.tilDto
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController(
    private val brevService: BrevService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}/pdf")
    fun hentBrevPdf(@PathVariable behandlingId: UUID): ByteArray {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Base64.getEncoder().encode(brevService.hentBrevPdf(behandlingId))
    }

    @PostMapping("/{behandlingId}")
    fun lagEllerOppdaterBrev(@PathVariable behandlingId: UUID): ByteArray {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Base64.getEncoder().encode(brevService.lagBrev(behandlingId))
    }

    @GetMapping("/{behandlingId}/mottakere")
    fun hentBrevmottakere(@PathVariable behandlingId: UUID): BrevmottakereDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return brevService.hentBrevmottakere(behandlingId).tilDto()
    }

    @PostMapping("/{behandlingId}/mottakere")
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody mottakere: BrevmottakereDto,
    ): BrevmottakereDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevService.settBrevmottakere(behandlingId, mottakere)
        return mottakere
    }
}
