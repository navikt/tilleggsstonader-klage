package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val oppgaveUri: URI = integrasjonerConfig.oppgaveUri

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/opprett")

        val respons = postForEntity<OppgaveResponse>(uri, opprettOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        return respons.oppgaveId
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$oppgaveUri/$oppgaveId/ferdigstill")
        patchForEntity<OppgaveResponse>(uri, "")
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri = URI.create("$oppgaveUri/${oppgave.id!!}/oppdater")
        val respons = patchForEntity<OppgaveResponse>(
            uri,
            oppgave,
            HttpHeaders().medContentTypeJsonUTF8(),
        )
        return respons.oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$oppgaveUri/$oppgaveId")

        val respons = getForEntity<Oppgave>(uri)
        return respons
    }

    fun finnMapper(enhetsnummer: String, limit: Int): FinnMappeResponseDto {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("mappe", "sok")
            .queryParam("enhetsnr", enhetsnummer)
            .queryParam("limit", limit)
            .build()
            .toUri()
        return getForEntity<FinnMappeResponseDto>(uri)
    }
}
