package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattRequest
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class TilleggsstonaderSakClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${TILLEGGSSTONADER_SAK_URL}") private val sakUrl: String,
) : AbstractRestClient(restTemplate) {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = URI.create("$sakUrl/api/klage/ekstern-fagsak/$fagsystemEksternFagsakId/vedtak").toString()
        return getForEntity<List<FagsystemVedtak>>(hentVedtakUri)
    }

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri =
            URI.create("$sakUrl/api/ekstern/behandling/kan-opprette-revurdering-klage/$fagsystemEksternFagsakId").toString()
        return getForEntity<KanOppretteRevurderingResponse>(hentVedtakUri)
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val hentVedtakUri =
            URI.create("$sakUrl/api/ekstern/behandling/opprett-revurdering-klage/$fagsystemEksternFagsakId").toString()
        return postForEntity<OpprettRevurderingResponse>(
            hentVedtakUri,
            emptyMap<String, String>()
        )
    }

    fun sjekkTilgangTilPersonMedRelasjoner(ident: String): Tilgang {
        return postForEntity<Tilgang>(
            URI.create("$sakUrl/api/tilgang/person/sjekkTilgangTilPersonMedRelasjoner").toString(),
            IdentRequest(ident = ident)
        )
    }

    fun erEgenAnsatt(ident: String): Boolean {
        return postForEntity<EgenAnsattResponse>(
            URI.create("$sakUrl/api/tilgang/person/erEgenAnsatt").toString(),
            EgenAnsattRequest(ident),
        ).erEgenAnsatt
    }
}
