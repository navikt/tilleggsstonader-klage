package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.getunleash.strategy.Strategy
import no.nav.tilleggsstonader.klage.infrastruktur.featuretoggle.ByEnvironmentStrategy
import no.nav.tilleggsstonader.klage.infrastruktur.featuretoggle.ByUserIdStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomUnleashStrategies {

    @Bean
    fun strategies(): List<Strategy> {
        return listOf(ByUserIdStrategy(), ByEnvironmentStrategy())
    }
}
