package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    integrasjonerConfig: IntegrasjonerConfig,
) : AbstractRestClient(restTemplate) {

    private val oppgaveUri: URI = integrasjonerConfig.oppgaveUri

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/opprett").toString()

        val respons = postForEntity<OppgaveResponse>(uri, opprettOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        return respons.oppgaveId
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("{oppgaveId}", "ferdigstill")
            .encode().toUriString()
        patchForEntity<OppgaveResponse>(uri, "", uriVariables = mapOf("oppgaveId" to oppgaveId))
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("{oppgaveId}", "oppdater")
            .encode().toUriString()
        val respons = patchForEntity<OppgaveResponse>(
            uri,
            oppgave,
            HttpHeaders().medContentTypeJsonUTF8(),
            mapOf("oppgaveId" to oppgave.id),
        )
        return respons.oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("{oppgaveId}")
            .encode().toUriString()
        return getForEntity<Oppgave>(uri, uriVariables = mapOf("oppgaveId" to oppgaveId))
    }

    fun finnMapper(enhetsnummer: String, limit: Int): FinnMappeResponseDto {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("mappe", "sok")
            .queryParam("enhetsnr", "{enhetsnr}")
            .queryParam("limit", "{limit}")
            .encode()
            .toUriString()
        return getForEntity<FinnMappeResponseDto>(
            uri,
            uriVariables = mapOf(
                "enhetsnr" to enhetsnummer,
                "limit" to limit,
            ),
        )
    }
}
