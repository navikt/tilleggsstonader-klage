package no.nav.tilleggsstonader.klage.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.libs.http.client.postForEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class FullmaktClient(
    @Qualifier("azure") private val restTemplate: RestTemplate,
    @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
) {
    private val uriFullmektige =
        UriComponentsBuilder
            .fromUri(integrasjonUri)
            .pathSegment("api", "fullmakt", "fullmektige")
            .encode()
            .toUriString()

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> =
        restTemplate.postForEntity(
            uri = uriFullmektige,
            payload = IdentRequest(fullmaktsgiversIdent),
        )
}
