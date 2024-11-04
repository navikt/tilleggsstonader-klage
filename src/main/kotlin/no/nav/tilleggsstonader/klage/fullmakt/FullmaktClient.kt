package no.nav.tilleggsstonader.klage.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class FullmaktClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
) : AbstractRestClient(restTemplate) {

    private val uriFullmektige = UriComponentsBuilder.fromUri(integrasjonUri)
        .pathSegment("api", "fullmakt", "fullmektige")
        .encode().toUriString()

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        return postForEntity(
            uri = uriFullmektige,
            payload = IdentRequest(fullmaktsgiversIdent),
        )
    }
}
