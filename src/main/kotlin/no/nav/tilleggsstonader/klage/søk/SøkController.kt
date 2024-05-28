package no.nav.tilleggsstonader.klage.søk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.gjeldende
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.visningsnavn
import no.nav.tilleggsstonader.klage.søk.dto.PersonIdentDto
import no.nav.tilleggsstonader.klage.søk.dto.PersonTreffDto
import no.nav.tilleggsstonader.klage.søk.ereg.EregService
import no.nav.tilleggsstonader.klage.søk.ereg.OrganisasjonDto
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(
    private val tilgangService: TilgangService,
    private val pdlClient: PdlClient,
    private val eregService: EregService,
    private val fagsakService: FagsakService,
) {

    @PostMapping("person")
    fun søkPerson(
        @RequestBody personIdentDto: PersonIdentDto,
    ): Ressurs<PersonTreffDto> {
        val personIdent = personIdentDto.personIdent
        val behandlingId = personIdentDto.behandlingId
        val fagsak = fagsakService.hentFagsak(behandlingId)
        tilgangService.validerTilgangTilPersonMedRelasjoner(personIdent, AuditLoggerEvent.UPDATE)
        val person = pdlClient.hentPerson(personIdent, fagsak.stønadstype)
        val result = PersonTreffDto(personIdent, person.navn.gjeldende().visningsnavn())
        return Ressurs.success(result)
    }

    @GetMapping("organisasjon/{organisasjonsnummer}")
    fun søkOrganisasjon(
        @PathVariable organisasjonsnummer: String,
    ): Ressurs<OrganisasjonDto> {
        if (!ORGNR_REGEX.matches(organisasjonsnummer)) {
            throw ApiFeil("Ugyldig organisasjonsnummer", HttpStatus.BAD_REQUEST)
        }
        return Ressurs.success(eregService.hentOrganisasjon(organisasjonsnummer))
    }

    companion object {

        private val ORGNR_REGEX = """\d{9}""".toRegex()
    }
}
