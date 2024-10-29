package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KabalClient(
    @Value("\${KABAL_URL}")
    private val kabalUrl: URI,
    @Qualifier("azure")
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val oversendelseUrl =
        UriComponentsBuilder.fromUri(kabalUrl)
            .pathSegment("api/oversendelse/v3/sak")
            .build().toUriString()

    fun sendTilKabal(oversendtKlage: OversendtKlageAnkeV3) {
        postForEntityNullable<Void>(oversendelseUrl, oversendtKlage)
    }
}
