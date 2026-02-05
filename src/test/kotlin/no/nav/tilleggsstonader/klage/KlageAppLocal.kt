package no.nav.tilleggsstonader.klage

import no.nav.tilleggsstonader.klage.infrastruktur.config.ApplicationConfig
import no.nav.tilleggsstonader.klage.infrastruktur.db.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class KlageAppLocal

fun main(args: Array<String>) {
    SpringApplicationBuilder(KlageAppLocal::class.java, ApplicationConfig::class.java)
        .initializers(DbContainerInitializer())
        .profiles(
            "local",
            "mock-integrasjoner",
            "mock-auditlogger",
            "mock-pdl",
            "mock-fullmakt",
            "mock-htmlify",
            "mock-tilleggsstonader-sak",
            "mock-dokument",
            "mock-kabal",
            "mock-ereg",
            // "mock-oppgave",
            "bruk-sak-oppgave",
            "mock-kafka",
        ).run(*args)
}
