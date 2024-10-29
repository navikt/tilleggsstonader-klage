package no.nav.tilleggsstonader.klage.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class IntegrasjonerConfig(@Value("\${TILLEGGSSTONADER_INTEGRASJONER_URL}") private val integrasjonUri: URI) {

    val oppgaveUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_OPPGAVE).build().toUri()

    val journalPostUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_JOURNALPOST).build().toUri()

    val saksbehandlerUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_SAKSBEHANDLER).build().toUri()

    val distribuerDokumentUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_DOKDIST).build().toUri()

    companion object {

        private const val PATH_OPPGAVE = "api/oppgave"
        private const val PATH_JOURNALPOST = "api/journalpost"
        private const val PATH_SAKSBEHANDLER = "api/saksbehandler"
        private const val PATH_DOKDIST = "api/dist"
    }
}
