package no.nav.tilleggsstonader.klage.integrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import java.net.URI

class TilleggsstønaderIntegrasjonerClientTest : IntegrationTest() {
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
    fun `distribuerJournalpost skal kunne parse streng-respons uten feilmeldinger fra jackson`() {

        val dummyBestillingsId = "351c137e-be74-4b43-9d0e-133efd0f4502"

        wireMockServer.stubFor(
            WireMock.post(WireMock.anyUrl())
                .willReturn(
                    WireMock.ok(dummyBestillingsId)
                        .withHeader("Content-Type", "application/json")
                ),
        )

        val integrasjonUri = URI.create("http://localhost:${wireMockServer.port()}")
        val response = TilleggsstønaderIntegrasjonerClient(
            restTemplateUtenAuth,
            integrasjonUri,
            IntegrasjonerConfig(integrasjonUri)
        ).distribuerJournalpost(
            DistribuerJournalpostRequest(
                journalpostId = "1",
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "appnavn",
                distribusjonstype = null
            )
        )

        assertThat(response).isEqualTo(dummyBestillingsId)
    }
}