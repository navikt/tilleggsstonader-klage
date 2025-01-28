package no.nav.tilleggsstonader.klage.personopplysninger

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/personopplysninger"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(
        @PathVariable behandlingId: UUID,
    ): PersonopplysningerDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return personopplysningerService.hentPersonopplysninger(behandlingId)
    }
}
