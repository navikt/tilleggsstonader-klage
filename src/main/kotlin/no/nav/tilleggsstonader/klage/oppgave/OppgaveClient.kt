package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.OppgaveConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.Optional

@Component
class OppgaveClient(
    @Qualifier("azure") restTemplate: RestTemplate,
    oppgaveConfig: OppgaveConfig,
) : AbstractRestClient(restTemplate) {
    private val oppgaveUri: URI = oppgaveConfig.oppgaveUri
    private val oppgaveVentUri: URI = oppgaveConfig.oppgaveVentUri

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/opprett").toString()

        val respons = postForEntity<OppgaveResponse>(uri, opprettOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        return respons.oppgaveId
    }

    fun ferdigstillOppgave(
        oppgaveId: Long,
        endretAvEnhetsnr: String?,
    ) {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("{oppgaveId}", "ferdigstill")
                .queryParamIfPresent("endretAvEnhetsnr", Optional.ofNullable(endretAvEnhetsnr))
                .encode()
                .toUriString()
        patchForEntity<OppgaveResponse>(uri, "", uriVariables = mapOf("oppgaveId" to oppgaveId))
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("{oppgaveId}", "oppdater")
                .encode()
                .toUriString()
        val respons =
            patchForEntity<OppgaveResponse>(
                uri,
                oppgave,
                HttpHeaders().medContentTypeJsonUTF8(),
                mapOf("oppgaveId" to oppgave.id),
            )
        return respons.oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("{oppgaveId}")
                .encode()
                .toUriString()
        return getForEntity<Oppgave>(uri, uriVariables = mapOf("oppgaveId" to oppgaveId))
    }

    fun finnMapper(
        enhetsnummer: String,
        limit: Int,
    ): FinnMappeResponseDto {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("mappe", "sok")
                .queryParam("enhetsnr", "{enhetsnr}")
                .queryParam("limit", "{limit}")
                .encode()
                .toUriString()
        return getForEntity<FinnMappeResponseDto>(
            uri,
            uriVariables =
                mapOf(
                    "enhetsnr" to enhetsnummer,
                    "limit" to limit,
                ),
        )
    }

    fun settPåVent(settPåVent: SettPåVentRequest): SettPåVentResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveVentUri)
                .pathSegment("sett-pa-vent")
                .encode()
                .toUriString()
        return kastBrukerFeilHvisBadRequest { postForEntity<SettPåVentResponse>(uri, settPåVent) }
    }

    fun oppdaterPåVent(oppdaterPåVent: OppdaterPåVentRequest): SettPåVentResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveVentUri)
                .pathSegment("oppdater-pa-vent")
                .encode()
                .toUriString()
        return kastBrukerFeilHvisBadRequest { postForEntity<SettPåVentResponse>(uri, oppdaterPåVent) }
    }

    fun taAvVent(taAvVent: TaAvVentRequest): SettPåVentResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveVentUri)
                .pathSegment("ta-av-vent")
                .encode()
                .toUriString()
        return kastBrukerFeilHvisBadRequest { postForEntity<SettPåVentResponse>(uri, taAvVent) }
    }

    private fun <T> kastBrukerFeilHvisBadRequest(fn: () -> T): T =
        try {
            fn()
        } catch (e: ProblemDetailException) {
            sjekkOgHåndtertConflict(e)
            val detail = e.detail.detail
            brukerfeilHvis(e.httpStatus == HttpStatus.BAD_REQUEST && detail != null) {
                detail ?: "Ukjent feil"
            }
            throw e
        }

    private fun sjekkOgHåndtertConflict(e: ProblemDetailException) {
        if (e.httpStatus == HttpStatus.CONFLICT) {
            throw ApiFeil(
                "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du laste inn siden på nytt",
                HttpStatus.CONFLICT,
            )
        }
    }
}
