package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.brev.HtmlifyClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-htmlify")
class HtmlifyClientMock {
    @Bean
    @Primary
    fun htmlifyClient(): HtmlifyClient {
        val htmlifyClient: HtmlifyClient = mockk()
        every { htmlifyClient.genererHtmlFritekstbrev(any(), any(), any()) } returns "<h1>Hei BESLUTTER_SIGNATUR</h1>"
        every { htmlifyClient.genererBlankett(any()) } returns "<h1>Hei Blankett</h1>"
        return htmlifyClient
    }
}
