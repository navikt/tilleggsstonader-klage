package no.nav.tilleggsstonader.klage

import no.nav.tilleggsstonader.klage.infrastruktur.config.ApplicationConfig
import no.nav.tilleggsstonader.klage.infrastruktur.db.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationConfig::class.java)
        .initializers(DbContainerInitializer())
        .profiles(
            "local",
            "mock-oauth",
            "mock-integrasjoner",
            "mock-auditlogger",
            "mock-pdl",
            "mock-brev",
            "mock-tilleggsstonader-sak",
            "mock-dokument",
            "mock-kabal",
            "mock-ereg",
            "mock-oppgave",
            "mock-kafka",
        )
        .run(*args)
}
