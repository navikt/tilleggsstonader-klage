package no.nav.tilleggsstonader.klage.vurdering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel.Companion.Hjemmeltema.TSO
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel.Companion.Hjemmeltema.TSR
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel.Companion.hjemlerRelevantFor
import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/hjemmel"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HjemmelController(
    private val fagsakService: FagsakService,
) {
    @GetMapping("tilgjenglige-hjemler/{behandlingId}")
    fun hentRelevanteHjemlerForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<Hjemmel> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (val behandlendeEnhet = fagsak.stønadstype.behandlendeEnhet()) {
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD,
            Enhet.NAV_ARBEID_OG_YTELSER_EGNE_ANSATTE,
            -> hjemlerRelevantFor(TSO)

            Enhet.NAV_TILTAK_OSLO,
            Enhet.NAV_EGNE_ANSATTE_OSLO,
            -> hjemlerRelevantFor(TSR)

            Enhet.VIKAFOSSEN,
            -> error("Kjenner ikke til hjemler for enhet: $behandlendeEnhet")
        }
    }
}
