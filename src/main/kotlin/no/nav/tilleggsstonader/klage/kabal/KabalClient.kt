package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.kabal.domain.OversendtKlageAnkeV4
import no.nav.tilleggsstonader.libs.http.client.postForEntityNullable
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
    private val restTemplate: RestTemplate,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val oversendelseUrl =
        UriComponentsBuilder
            .fromUri(kabalUrl)
            .pathSegment("api/oversendelse/v4/sak")
            .build()
            .toUriString()

    fun sendTilKabal(oversendtKlage: OversendtKlageAnkeV4) {
        restTemplate.postForEntityNullable<Void>(oversendelseUrl, oversendtKlage)
    }
}
