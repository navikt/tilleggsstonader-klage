package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.blankett.BlankettPdfRequest
import no.nav.tilleggsstonader.klage.brev.dto.FritekstBrevRequestDto
import no.nav.tilleggsstonader.libs.http.client.postForEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class HtmlifyClient(
    @Value("\${TILLEGGSSTONADER_HTMLIFY_URL}")
    private val uri: URI,
    @Qualifier("utenAuth")
    private val restTemplate: RestTemplate,
) {
    private val interntVedtakUri = URI.create("$uri/api/klage/internt-vedtak").toString()
    private val fritekstBrevUri = URI.create("$uri/api/klage/fritekst-brev").toString()

    fun genererHtmlFritekstbrev(
        fritekstBrev: FritekstBrevRequestDto,
        saksbehandlerNavn: String,
        enhet: String,
    ): String =
        restTemplate.postForEntity(
            fritekstBrevUri,
            FritekstBrevRequestMedSignatur(
                fritekstBrev,
                saksbehandlerNavn,
                enhet,
            ),
            HttpHeaders(),
        )

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): String =
        restTemplate.postForEntity(interntVedtakUri, blankettPdfRequest, HttpHeaders())
}

data class FritekstBrevRequestMedSignatur(
    val brevFraSaksbehandler: FritekstBrevRequestDto,
    val saksbehandlersignatur: String,
    val enhet: String,
)
