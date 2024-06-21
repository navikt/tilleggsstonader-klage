package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.getDataOrThrow
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilleggsstonaderSakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${TILLEGGSSTONADER_SAK_URL}") private val sakUrl: String,
) : AbstractRestClient(restOperations, "tilleggsstonader.sak") {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = UriComponentsBuilder.fromUri(TSSakUri)
            .pathSegment("api/klage/ekstern-fagsak/$fagsystemEksternFagsakId/vedtak")
            .build().toUri()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri = UriComponentsBuilder.fromUri(TSSakUri)
            .pathSegment("api/ekstern/behandling/kan-opprette-revurdering-klage/$fagsystemEksternFagsakId")
            .build().toUri()
        return getForEntity<Ressurs<KanOppretteRevurderingResponse>>(hentVedtakUri).getDataOrThrow()
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val hentVedtakUri = UriComponentsBuilder.fromUri(TSSakUri)
            .pathSegment("api/ekstern/behandling/opprett-revurdering-klage/$fagsystemEksternFagsakId")
            .build().toUri()
        return postForEntity<Ressurs<OpprettRevurderingResponse>>(hentVedtakUri, emptyMap<String, String>()).getDataOrThrow()
    }
}
