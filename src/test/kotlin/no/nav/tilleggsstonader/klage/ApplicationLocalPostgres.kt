package no.nav.tilleggsstonader.klage

import no.nav.tilleggsstonader.klage.infrastruktur.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import
import java.util.Properties

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@Import(ApplicationConfig::class)
class ApplicationLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5433/tilleggsstonader-klage"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(ApplicationLocalPostgres::class.java)
        .profiles(
            "local",
            "mock-oauth",
            "mock-integrasjoner",
            "mock-pdl",
            "mock-brev",
            "mock-dokument",
            "mock-tilleggsstonader-sak",
            "mock-kabal",
            "mock-ereg",
            "mock-inntekt",
            "mock-oppgave",
            "mock-kafka",
            )
        .properties(properties)
        .run(*args)
}
