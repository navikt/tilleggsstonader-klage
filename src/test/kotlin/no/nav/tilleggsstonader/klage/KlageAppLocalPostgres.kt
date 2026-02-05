package no.nav.tilleggsstonader.klage

import no.nav.tilleggsstonader.klage.infrastruktur.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Import
import java.util.Properties

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@Import(ApplicationConfig::class)
class KlageAppLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5433/tilleggsstonader-klage"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(KlageAppLocalPostgres::class.java)
        .profiles(
            "local",
            "mock-integrasjoner",
            "mock-pdl",
            "mock-fullmakt",
            "mock-htmlify",
            "mock-dokument",
            "mock-tilleggsstonader-sak",
            "mock-kabal",
            "mock-ereg",
            "mock-inntekt",
            "mock-oppgave",
            "mock-kafka",
        ).properties(properties)
        .run(*args)
}
