package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.log.NavHttpHeaders
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilleggsstønaderIntegrasjonerClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig,

) : AbstractRestClient(restTemplate) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val dokArkivUri =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/arkiv").build().toUriString()
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    fun arkiverDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?,
    ): ArkiverDokumentResponse =
        postForEntity<ArkiverDokumentResponse>(
            dokArkivUri,
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler),
        )

    fun distribuerJournalpost(request: DistribuerJournalpostRequest, saksbehandler: String? = null): String {
        return postForEntity<String>(
            uri = integrasjonerConfig.distribuerDokumentUri.toString(),
            payload = request,
            httpHeaders = headerMedSaksbehandler(saksbehandler),
        )
    }

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> =
        postForEntity<List<Journalpost>>(journalpostURI.toString(), journalposterForBrukerRequest)

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler {
        val uri = UriComponentsBuilder.fromUri(saksbehandlerUri)
            .pathSegment("{navIdent}")
            .encode().toUriString()
        return getForEntity<Saksbehandler>(
            uri,
            HttpHeaders().medContentTypeJsonUTF8(),
            mapOf("navIdent" to navIdent),
        )
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }
}
