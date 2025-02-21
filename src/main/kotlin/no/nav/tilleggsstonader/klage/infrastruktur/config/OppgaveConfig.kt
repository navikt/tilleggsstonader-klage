package no.nav.tilleggsstonader.klage.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class OppgaveConfig(
    @Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}") private val integrasjonUri: URI,
) {
    val oppgaveUri: URI =
        UriComponentsBuilder
            .fromUri(integrasjonUri)
            .pathSegment(PATH_OPPGAVE)
            .build()
            .toUri()

    companion object {
        private const val PATH_OPPGAVE = "api/oppgave"
    }
}
