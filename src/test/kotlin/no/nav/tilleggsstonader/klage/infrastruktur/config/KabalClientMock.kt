package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.kabal.KabalClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-kabal")
class KabalClientMock {
    @Bean
    @Primary
    fun kabalClient(): KabalClient {
        val kabalClient: KabalClient = mockk()
        every { kabalClient.sendTilKabal(any()) } just Runs
        return kabalClient
    }
}
