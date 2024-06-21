package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattRequest
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.getDataOrThrow
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
        return getForEntity<Ressurs<KanOppretteRevurderingResponse>>(hentVedtakUri).getDataOrThrow()
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val hentVedtakUri =
            URI.create("$sakUrl/api/ekstern/behandling/opprett-revurdering-klage/$fagsystemEksternFagsakId")
        return postForEntity<Ressurs<OpprettRevurderingResponse>>(
            hentVedtakUri,
            emptyMap<String, String>()
        ).getDataOrThrow()
    }

    fun sjekkTilgangTilPersonMedRelasjoner(ident: String): Tilgang {
        return getForEntity(
            URI.create("$sakUrl/api/tilgang/person/$ident/sjekkTilgangTilPersonMedRelasjoner"),
        )
    }

    fun erEgenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(
            URI.create("$sakUrl/api/tilgang/person/$ident/erEgenAnsatt"),
            EgenAnsattRequest(ident),
        ).data!!.erEgenAnsatt
    }
}
