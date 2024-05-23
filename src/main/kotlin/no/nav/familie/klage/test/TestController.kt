package no.nav.tilleggsstonader.klage.test

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakPersonService
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TestController(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakRepository: FagsakRepository,
    private val opprettBehandlingService: OpprettBehandlingService,
) {

    @PostMapping("opprett")
    fun opprettDummybehandling(@RequestBody request: DummybehandlingRequest): Ressurs<UUID> {
        val fagsakPerson = fagsakPersonService.hentEllerOpprettPerson(setOf(request.ident), request.ident)
        // findByEksternIdAndFagsystemAndStønadstype ?
        val eksternFagsakId = fagsakRepository.findAll()
            .find { it.fagsakPersonId == fagsakPerson.id && it.stønadstype == request.stønadstype }
            ?.eksternId ?: UUID.randomUUID().toString()

        return Ressurs.success(
            opprettBehandlingService.opprettBehandling(
                OpprettKlagebehandlingRequest(
                    request.ident,
                    request.stønadstype,
                    eksternFagsakId,
                    request.fagsystem,
                    request.klageMottatt,
                    request.behandlendeEnhet,
                ),
            ),
        )
    }

    data class DummybehandlingRequest(
        val ident: String,
        val stønadstype: Stønadstype,
        val fagsystem: Fagsystem = Fagsystem.EF,
        val klageMottatt: LocalDate = LocalDate.now(),
        val behandlendeEnhet: String = "4489",
    )
}
