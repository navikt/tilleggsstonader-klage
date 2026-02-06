package no.nav.tilleggsstonader.klage.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.tilleggsstonader.klage.infrastruktur.config.PdfMock.PDF_AS_BASE64_STRING
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.journalpost.RelevantDato
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@Component
class IntegrasjonerMock(
    integrasjonerConfig: IntegrasjonerConfig,
) {
    private val responses =
        listOf(
            get(urlPathMatching("${integrasjonerConfig.saksbehandlerUri.path}/([A-Za-z0-9]*)"))
                .willReturn(okJson(jsonMapper.writeValueAsString(saksbehandler))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("1234"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpost))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("2345"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpostPapirsøknad))),
            post(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalposter))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(okJson(jsonMapper.writeValueAsString(dummyPdf))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(jsonMapper.writeValueAsString(PDF_AS_BASE64_STRING))),
            post(urlEqualTo(integrasjonerConfig.distribuerDokumentUri.path))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            "123",
                        ),
                    ).withStatus(200),
                ),
        )

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(): WireMockServer {
        val mockServer = WireMockServer(8386)
        responses.forEach {
            mockServer.stubFor(it)
        }
        mockServer.start()
        return mockServer
    }

    @Bean
    @Profile("mock-integrasjoner")
    @Primary
    fun tilleggsstønaderIntegrasjonerClient(
        @Qualifier("utenAuth") restTemplate: RestTemplate,
        @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}")
        integrasjonUri: URI,
        integrasjonerConfig: IntegrasjonerConfig,
    ): TilleggsstønaderIntegrasjonerClient =
        TilleggsstønaderIntegrasjonerClient(
            restTemplate,
            integrasjonUri,
            integrasjonerConfig,
        )

    companion object {
        private const val DUMMY_FNR = "23097825289"

        private val journalpostFraIntegrasjoner =
            Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = DUMMY_FNR),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 1",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ORIGINAL,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om barnetilsyn - dokument 1",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Samboeravtale",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Manuelt skannet dokument",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                            logiskeVedlegg =
                                listOf(
                                    LogiskVedlegg(
                                        logiskVedleggId = "1",
                                        tittel = "Manuelt skannet samværsavtale",
                                    ),
                                    LogiskVedlegg(
                                        logiskVedleggId = "2",
                                        tittel = "Annen fritekst fra gosys",
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 2",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 3",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                    ),
            )
        private val journalpostPapirsøknadFraIntegrasjoner =
            Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = DUMMY_FNR),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 1",
                            brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                    ),
            )

        private val dummyPdf =
            this::class.java.classLoader
                .getResource("dummy/pdf_dummy.pdf")!!
                .readBytes()
        private val journalpost = journalpostFraIntegrasjoner
        private val journalpostPapirsøknad = journalpostPapirsøknadFraIntegrasjoner
        private val journalposter = listOf(journalpostFraIntegrasjoner)
        private val saksbehandler =
            Saksbehandler(
                azureId = UUID.randomUUID(),
                navIdent = "Z999999",
                fornavn = "Darth",
                etternavn = "Vader",
                enhet = "4405",
            )
    }
}
