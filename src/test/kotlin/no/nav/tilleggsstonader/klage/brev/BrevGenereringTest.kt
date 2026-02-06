package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.blankett.BlankettFormDto
import no.nav.tilleggsstonader.klage.blankett.BlankettPdfBehandling
import no.nav.tilleggsstonader.klage.blankett.BlankettPdfRequest
import no.nav.tilleggsstonader.klage.blankett.BlankettPåklagetVedtakDto
import no.nav.tilleggsstonader.klage.blankett.BlankettVurderingDto
import no.nav.tilleggsstonader.klage.blankett.PersonopplysningerDto
import no.nav.tilleggsstonader.klage.brev.dto.AvsnittDto
import no.nav.tilleggsstonader.klage.brev.dto.FritekstBrevRequestDto
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.domain.FormkravFristUnntak
import no.nav.tilleggsstonader.klage.util.FileUtil
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import java.io.File
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * For å teste breven:
 * * Ta inn @Disabled
 * * Kjør tester
 * * Formatter html
 * * Kommenter ut @Disabled
 * * Commit
 */
class BrevGenereringTest {
    val htmlifyClient: HtmlifyClient = lagHtmlifyClient()
    val familieDokumentClient: FamilieDokumentClient = lagFamilieDokumentClient()

    @Disabled
    @Test
    fun `generer internt vedtak`() {
        val html = htmlifyClient.genererBlankett(BlankettTestData.request)
        val filnavn = "brev/blankett"

        skrivTilFil("$filnavn.html", html)
        skrivTilFil("$filnavn.pdf", genererPdf(html))

        assertThat(true).withFailMessage { "Skal ikke kjøre denne testen på CI" }.isFalse()
    }

    @Disabled
    @Test
    fun `generer fritekst brev`() {
        val html =
            htmlifyClient.genererHtmlFritekstbrev(
                fritekstBrev = FriktestBrevData.request,
                saksbehandlerNavn = "Saksbehandler Navn",
                enhet = "En enhet",
            )
        val filnavn = "brev/fritekst-brev"

        skrivTilFil("$filnavn.html", html)
        skrivTilFil("$filnavn.pdf", genererPdf(html))

        assertThat(true).withFailMessage { "Skal ikke kjøre denne testen på CI" }.isFalse()
    }

    @Test
    fun `html skal være formatert for å enklere kunne sjekke diff`() {
        val rootFolder = "brev"
        val filer = FileUtil.listFiles(rootFolder)
        assertThat(filer).isNotEmpty
        filer.forEach { file ->
            val fil = FileUtil.readFile("$rootFolder/${file.fileName}")
            val erIkkeFormatert =
                fil
                    .split("\n")
                    .none { it.contains("<body") && it.contains("<div") }
            assertThat(erIkkeFormatert).isTrue()
        }
    }

    private fun genererPdf(html: String): ByteArray = familieDokumentClient.genererPdfFraHtml(html)

    private fun skrivTilFil(
        filnavn: String,
        data: String,
    ) {
        skrivTilFil(filnavn, data.toByteArray())
    }

    private fun skrivTilFil(
        filnavn: String,
        data: ByteArray,
    ) {
        val file = File("src/test/resources/$filnavn")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeBytes(data)
    }

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters =
            listOf(
                StringHttpMessageConverter(),
                JacksonJsonHttpMessageConverter(jsonMapper),
            )
        return HtmlifyClient(uri = URI.create("http://localhost:8001"), restTemplate = restTemplate)
    }

    private fun lagFamilieDokumentClient(): FamilieDokumentClient {
        val restTemplate = TestRestTemplate().restTemplate
        return FamilieDokumentClient(
            familieDokumentUrl = "https://familie-dokument.intern.dev.nav.no",
            restTemplate = restTemplate,
        )
    }
}

private object FriktestBrevData {
    val request =
        FritekstBrevRequestDto(
            overskrift = "Overskrift til brev",
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "Deloverskrift 1",
                        innhold = "Innholt til avsnitt",
                    ),
                ),
            personIdent = "11111122222",
            navn = "Fornavn etternavn",
        )
}

private object BlankettTestData {
    private val behandling =
        BlankettPdfBehandling(
            eksternFagsakId = "eksternFagsakId",
            stønadstype = Stønadstype.LÆREMIDLER,
            klageMottatt = LocalDate.of(2025, 1, 1),
            resultat = BehandlingResultat.MEDHOLD,
            påklagetVedtak =
                BlankettPåklagetVedtakDto(
                    behandlingstype = "behandlingstype",
                    resultat = "resultat",
                    vedtakstidspunkt = LocalDateTime.of(2024, 12, 24, 8, 12, 0),
                ),
        )
    private val personopplysninger =
        PersonopplysningerDto(
            navn = "Fornavn etternavn",
            personIdent = "11111122222",
        )
    private val formkrav =
        BlankettFormDto(
            klagePart = FormVilkår.OPPFYLT,
            klageKonkret = FormVilkår.IKKE_SATT,
            klagefristOverholdt = FormVilkår.IKKE_OPPFYLT,
            klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK,
            klageSignert = FormVilkår.OPPFYLT,
            saksbehandlerBegrunnelse = "En begrunnelse",
            brevtekst = "brevtekst",
        )
    private val vurdering =
        BlankettVurderingDto(
            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
            årsak = Årsak.FEIL_PROSESSUELL,
            begrunnelseOmgjøring = "begrunnelse omgjøring",
            hjemler = listOf(Hjemmel.FL_10),
            innstillingKlageinstans = "Instilling",
            interntNotat = "Internt notat",
        )

    val request =
        BlankettPdfRequest(
            behandling = behandling,
            personopplysninger = personopplysninger,
            formkrav = formkrav,
            vurdering = vurdering,
        )
}
