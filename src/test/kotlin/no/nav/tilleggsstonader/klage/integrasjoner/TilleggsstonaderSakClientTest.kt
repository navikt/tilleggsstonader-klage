package no.nav.tilleggsstonader.klage.integrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDateTime

class TilleggsstonaderSakClientTest : IntegrationTest() {
    @Autowired
    @Qualifier("utenAuth")
    lateinit var restTemplateUtenAuth: RestTemplate

    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }
    @Test
    fun `hentVedtak fra sak skal kunne parse streng-respons uten feilmeldinger fra jackson`() {
        val eksternFagsakId = "351c137e-be74-4b43-9d0e-133efd0f4502"
        val fagsystemvedtak = FagsystemVedtak(eksternBehandlingId =  eksternFagsakId, behandlingstype = "test", fagsystemType = FagsystemType.ORDNIÆR, regelverk = Regelverk.NASJONAL, resultat = "test", vedtakstidspunkt = LocalDateTime.now())
        val contentResponse : List<FagsystemVedtak> = listOf(fagsystemvedtak)


        wireMockServer.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(
                    WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(Ressurs<List<FagsystemVedtak>>(melding ="lala", data= contentResponse, status = Ressurs.Status.SUKSESS, stacktrace = "olalla" ).toJson())
                ),
        )
        val sakUri = URI.create("http://localhost:${wireMockServer.port()}").toString()
        val response = TilleggsstonaderSakClient(restTemplateUtenAuth, sakUrl = sakUri).hentVedtak(eksternFagsakId)

        assertThat(response[0].eksternBehandlingId).isEqualTo(eksternFagsakId)

    }
}