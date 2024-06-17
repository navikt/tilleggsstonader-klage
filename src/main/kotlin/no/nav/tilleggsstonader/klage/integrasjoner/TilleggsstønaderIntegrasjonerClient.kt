package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.log.NavHttpHeaders
import no.nav.tilleggsstonader.klage.Saksbehandler
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilleggsstønaderIntegrasjonerClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig,

    ) : AbstractPingableRestClient(restOperations, "journalpost") {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override val pingUri: URI = URI.create("/api/ping")

    private val dokuarkivUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/arkiv").build().toUri()
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    fun arkiverDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?
    ): ArkiverDokumentResponse =
        postForEntity<ArkiverDokumentResponse>(
            URI.create("$dokuarkivUri"),
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler),
        )

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler = getForEntity<Saksbehandler>(
        URI.create("$saksbehandlerUri/$navIdent"),
        HttpHeaders().medContentTypeJsonUTF8(),
    )

    fun distribuerBrev(journalpostId: String, distribusjonstype: Distribusjonstype): String =
        postForEntity<String>(
            integrasjonerConfig.distribuerDokumentUri,
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "TILLEGGSSTONADER-KLAGE",
                distribusjonstype = distribusjonstype,
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> =
        postForEntity<List<Journalpost>>(journalpostURI, journalposterForBrukerRequest)

    fun hentJournalpost(journalpostId: String): Journalpost =
        getForEntity<Journalpost>(URI.create("$journalpostURI?journalpostId=$journalpostId"))

    // TODO: kastApiFeilDersomUtviklerMedVeilederrolle() for å ikke gi tilgang til dokumenter med feil tema i prod
    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray =
        getForEntity<ByteArray>(
            UriComponentsBuilder
                .fromUriString(
                    "$journalpostURI/hentdokument/" +
                            "$journalpostId/$dokumentInfoId",
                )
                .queryParam("variantFormat", Dokumentvariantformat.ARKIV)
                .build()
                .toUri(),
        )

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }
}
