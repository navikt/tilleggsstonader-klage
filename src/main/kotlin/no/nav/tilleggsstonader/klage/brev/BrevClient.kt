package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.blankett.BlankettPdfRequest
import no.nav.tilleggsstonader.klage.brev.dto.FritekstBrevRequestDto
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class BrevClient(
    @Value("\${FAMILIE_BREV_API_URL}")
    private val familieBrevUri: URI,
    @Qualifier("utenAuth")
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    private val pdfUri = URI.create("$familieBrevUri/blankett/klage/pdf").toString()
    private val genererHtmlFritekstbrevUri = URI.create("$familieBrevUri/api/fritekst-brev/html").toString()

    fun genererHtmlFritekstbrev(
        fritekstBrev: FritekstBrevRequestDto,
        saksbehandlerNavn: String,
        enhet: String,
    ): String =
        postForEntity(
            genererHtmlFritekstbrevUri,
            FritekstBrevRequestMedSignatur(
                fritekstBrev,
                saksbehandlerNavn,
                enhet,
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray =
        postForEntity(pdfUri, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
}

data class FritekstBrevRequestMedSignatur(
    val brevFraSaksbehandler: FritekstBrevRequestDto,
    val saksbehandlersignatur: String,
    val enhet: String,
)
