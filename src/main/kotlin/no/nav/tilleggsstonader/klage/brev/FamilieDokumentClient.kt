package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class FamilieDokumentClient(
    @Value("\${FAMILIE_DOKUMENT_URL}")
    private val familieDokumentUrl: String,
    @Qualifier("utenAuth")
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    fun genererPdfFraHtml(html: String): ByteArray {
        val htmlTilPdfURI = URI.create("$familieDokumentUrl/$HTML_TIL_PDF").toString()

        return postForEntity(
            uri = htmlTilPdfURI,
            payload = html.encodeToByteArray(),
            httpHeaders =
                HttpHeaders().apply {
                    this.contentType = MediaType.TEXT_HTML
                    this.accept = listOf(MediaType.APPLICATION_PDF)
                },
        )
    }

    companion object {
        const val HTML_TIL_PDF = "api/html-til-pdf"
    }
}
