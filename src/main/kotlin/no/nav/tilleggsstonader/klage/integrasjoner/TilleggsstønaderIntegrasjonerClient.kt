package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.http.client.RessursException
import no.nav.familie.log.NavHttpHeaders
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.Saksbehandler
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.getDataOrThrow
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ApiFeil
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
import org.springframework.http.HttpStatus
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

    private val dokuarkivUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/arkiv").build().toUri()
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    fun arkiverDokument(arkiverDokumentRequest: ArkiverDokumentRequest, saksbehandler: String?): ArkiverDokumentResponse {
        return postForEntity<Ressurs<ArkiverDokumentResponse>>(
            URI.create("$dokuarkivUri"),
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler),
        ).data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler {
        return getForEntity<Ressurs<Saksbehandler>>(
            URI.create("$saksbehandlerUri/$navIdent"),
            HttpHeaders().medContentTypeJsonUTF8(),
        ).data
            ?: error("Kunne ikke hente saksbehandlerinfo for saksbehandler med ident=$navIdent")
    }

    // sende brev til bruker
    fun distribuerBrev(journalpostId: String, distribusjonstype: Distribusjonstype): String {
        val journalpostRequest = DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
            dokumentProdApp = "FAMILIE_KLAGE",
            distribusjonstype = distribusjonstype,
        )

        return postForEntity<Ressurs<String>>(
            integrasjonerConfig.distribuerDokumentUri,
            journalpostRequest,
            HttpHeaders().medContentTypeJsonUTF8(),
        ).getDataOrThrow()
    }

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        return postForEntity<Ressurs<List<Journalpost>>>(journalpostURI, journalposterForBrukerRequest).data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerRequest.brukerId.id}")
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        val ressurs = try {
            getForEntity<Ressurs<Journalpost>>(URI.create("$journalpostURI?journalpostId=$journalpostId"))
        } catch (e: RessursException) {
            if (e.message?.contains("Fant ikke journalpost i fagarkivet") == true) {
                throw ApiFeil("Finner ikke journalpost i fagarkivet", HttpStatus.BAD_REQUEST)
            } else {
                throw e
            }
        }
        return ressurs.getDataOrThrow()
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(
            UriComponentsBuilder
                .fromUriString(
                    "$journalpostURI/hentdokument/" +
                        "$journalpostId/$dokumentInfoId",
                )
                .queryParam("variantFormat", Dokumentvariantformat.ARKIV)
                .build()
                .toUri(),
        )
            .getDataOrThrow()
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }
}
