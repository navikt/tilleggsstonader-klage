package no.nav.tilleggsstonader.klage.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.Saksbehandler
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.infrastruktur.config.PdfMock.pdfAsBase64String
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.journalpost.RelevantDato
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.random.Random

@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    private val responses =
        listOf(
            get(urlEqualTo(integrasjonerConfig.pingUri.path))
                .willReturn(aResponse().withStatus(200)),
            get(urlPathMatching("${integrasjonerConfig.saksbehandlerUri.path}/([A-Za-z0-9]*)"))
                .willReturn(okJson(objectMapper.writeValueAsString(saksbehandler))),
            post(urlEqualTo(integrasjonerConfig.egenAnsattUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(egenAnsatt))),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .withRequestBody(matching(".*ikkeTilgang.*"))
                .atPriority(1)
                .willReturn(okJson(objectMapper.writeValueAsString(lagIkkeTilgangResponse()))),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(Tilgang(true, null)))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("1234"))
                .willReturn(okJson(objectMapper.writeValueAsString(journalpost))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("2345"))
                .willReturn(okJson(objectMapper.writeValueAsString(journalpostPapirsøknad))),
            post(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(journalposter))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(dummyPdf)))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(pdfAsBase64String)))),
            post(urlEqualTo(integrasjonerConfig.distribuerDokumentUri.path))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                "123",
                            ),
                        ),
                    ).withStatus(200),
                ),
            post(urlEqualTo("${integrasjonerConfig.oppgaveUri.path}/opprett"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(OppgaveResponse(Random.nextLong().absoluteValue))))),
            patch(urlPathMatching("${integrasjonerConfig.oppgaveUri.path}/([0-9]*)/ferdigstill"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(OppgaveResponse(Random.nextLong().absoluteValue))))),

        )

    private fun lagIkkeTilgangResponse() = Tilgang(
        false,
        "Mock sier: Du har " +
            "ikke tilgang " +
            "til person ikkeTilgang",
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

    companion object {

        private val egenAnsatt = Ressurs.success(EgenAnsattResponse(false))

        private const val fnr = "23097825289"

        private val journalpostFraIntegrasjoner =
            Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
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
                            Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true),
                            Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL, saksbehandlerHarTilgang = true),
                        ),
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om barnetilsyn - dokument 1",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Samboeravtale",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Manuelt skannet dokument",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        logiskeVedlegg = listOf(
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
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om overgangsstønad - dokument 2",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om overgangsstønad - dokument 3",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
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
                bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
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
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                ),
            )

        private val dummyPdf = this::class.java.classLoader.getResource("dummy/pdf_dummy.pdf")!!.readBytes()
        private val journalpost = Ressurs.success(journalpostFraIntegrasjoner)
        private val journalpostPapirsøknad = Ressurs.success(journalpostPapirsøknadFraIntegrasjoner)
        private val journalposter = Ressurs.success(listOf(journalpostFraIntegrasjoner))
        private val saksbehandler = Ressurs.success(
            Saksbehandler(
                azureId = UUID.randomUUID(),
                navIdent = "Z999999",
                fornavn = "Darth",
                etternavn = "Vader",
                enhet = "4405",
            ),
        )
    }
}