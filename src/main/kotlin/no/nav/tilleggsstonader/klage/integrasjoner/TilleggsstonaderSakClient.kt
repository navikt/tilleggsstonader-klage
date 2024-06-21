package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattRequest
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.getDataOrThrow
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class TilleggsstonaderSakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${TILLEGGSSTONADER_SAK_URL}") private val sakUrl: String,
) : AbstractRestClient(restOperations, "tilleggsstonader.sak") {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = URI.create("$sakUrl/api/klage/ekstern-fagsak/$fagsystemEksternFagsakId/vedtak")
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri =
            URI.create("$sakUrl/api/ekstern/behandling/kan-opprette-revurdering-klage/$fagsystemEksternFagsakId")
        return getForEntity<KanOppretteRevurderingResponse>(hentVedtakUri)
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val hentVedtakUri =
            URI.create("$sakUrl/api/ekstern/behandling/opprett-revurdering-klage/$fagsystemEksternFagsakId")
        return postForEntity<OpprettRevurderingResponse>(
            hentVedtakUri,
            emptyMap<String, String>()
        )
    }

    fun sjekkTilgangTilPersonMedRelasjoner(ident: String): Tilgang {
        return postForEntity<Tilgang>(
            URI.create("$sakUrl/api/tilgang/person/sjekkTilgangTilPersonMedRelasjoner"),
            IdentRequest(ident = ident)
        )
    }

    fun erEgenAnsatt(ident: String): Boolean {
        return postForEntity<EgenAnsattResponse>(
            URI.create("$sakUrl/api/tilgang/person/erEgenAnsatt"),
            EgenAnsattRequest(ident),
        ).erEgenAnsatt
    }
}
