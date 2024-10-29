package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattRequest
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.getDataOrThrow
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilleggsstonaderSakClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${TILLEGGSSTONADER_SAK_URL}") private val sakUri: URI,
) : AbstractRestClient(restTemplate) {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val uri = UriComponentsBuilder.fromUri(sakUri)
            .pathSegment("api", "klage", "ekstern-fagsak", "{fagsystemEksternFagsakId}", "vedtak")
            .encode().toUriString()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(
            uri,
            uriVariables = mapOf("fagsystemEksternFagsakId" to fagsystemEksternFagsakId),
        ).getDataOrThrow()
    }

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse {
        val uri = UriComponentsBuilder.fromUri(sakUri)
            .pathSegment("api", "ekstern", "behandling", "kan-opprette-revurdering-klage", "{fagsystemEksternFagsakId}")
            .encode().toUriString()
        return getForEntity<KanOppretteRevurderingResponse>(
            uri,
            uriVariables = mapOf("fagsystemEksternFagsakId" to fagsystemEksternFagsakId),
        )
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val uri = UriComponentsBuilder.fromUri(sakUri)
            .pathSegment("api", "ekstern", "behandling", "opprett-revurdering-klage", "{fagsystemEksternFagsakId}")
            .encode().toUriString()
        return postForEntity<OpprettRevurderingResponse>(
            uri,
            emptyMap<String, String>(),
            uriVariables = mapOf("fagsystemEksternFagsakId" to fagsystemEksternFagsakId),
        )
    }

    fun sjekkTilgangTilPersonMedRelasjoner(ident: String): Tilgang {
        return postForEntity<Tilgang>(
            URI.create("$sakUri/api/tilgang/person/sjekkTilgangTilPersonMedRelasjoner").toString(),
            IdentRequest(ident = ident),
        )
    }

    fun erEgenAnsatt(ident: String): Boolean {
        return postForEntity<EgenAnsattResponse>(
            URI.create("$sakUri/api/tilgang/person/erEgenAnsatt").toString(),
            EgenAnsattRequest(ident),
        ).erEgenAnsatt
    }
}
