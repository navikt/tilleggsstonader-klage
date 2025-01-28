package no.nav.tilleggsstonader.klage.infrastruktur.config

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.tilleggsstonader.klage.infrastruktur.filter.NAVIdentFilter
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.libs.http.client.RetryOAuth2HttpClient
import no.nav.tilleggsstonader.libs.http.config.RestTemplateConfiguration
import no.nav.tilleggsstonader.libs.log.filter.LogFilterConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.familie.prosessering")
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@Import(RestTemplateConfiguration::class, LogFilterConfiguration::class)
@EnableOAuth2Client(cacheEnabled = true)
@EnableScheduling
class ApplicationConfig {
    /**
     * Overskrever OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient =
        RetryOAuth2HttpClient(
            RestClient.create(
                RestTemplateBuilder()
                    .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                    .setReadTimeout(Duration.of(2, ChronoUnit.SECONDS))
                    .build(),
            ),
        )

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            try {
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("azuread")
                    .getStringClaim("preferred_username")
            } catch (e: Exception) {
                throw e
            }

        override fun harTilgang(): Boolean = SikkerhetContext.harRolle(prosesseringRolle)
    }

    @Bean
    fun navIdentFilter(): FilterRegistrationBean<NAVIdentFilter> {
        val filterRegistration = FilterRegistrationBean<NAVIdentFilter>()
        filterRegistration.filter = NAVIdentFilter()
        filterRegistration.order = 1 // Samme nivå som LogFilter sånn at navIdent blir med på RequestTimeFilter
        return filterRegistration
    }
}
