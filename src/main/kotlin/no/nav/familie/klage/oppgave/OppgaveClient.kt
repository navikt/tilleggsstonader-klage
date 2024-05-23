package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.http.client.AbstractPingableRestClient
import no.nav.tilleggsstonader.klage.felles.util.medContentTypeJsonUTF8
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.IntegrasjonException
import no.nav.tilleggsstonader.kontrakter.felles.Ressurs
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
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

        val respons = postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        return pakkUtRespons(respons, uri, "opprettOppgave").oppgaveId
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$oppgaveUri/$oppgaveId/ferdigstill")
        val respons = patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
        pakkUtRespons(respons, uri, "ferdigstillOppgave")
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri = URI.create("$oppgaveUri/${oppgave.id!!}/oppdater")
        val respons = patchForEntity<Ressurs<OppgaveResponse>>(
            uri,
            oppgave,
            HttpHeaders().medContentTypeJsonUTF8(),
        )
        return pakkUtRespons(respons, uri, "oppdaterOppgave").oppgaveId
    }

    private fun <T> pakkUtRespons(
        respons: Ressurs<T>,
        uri: URI?,
        metode: String,
    ): T {
        val data = respons.data
        if (respons.status == Ressurs.Status.SUKSESS && data != null) {
            return data
        } else if (respons.status == Ressurs.Status.SUKSESS) {
            throw IntegrasjonException("Ressurs har status suksess, men mangler data")
        } else {
            throw IntegrasjonException(
                "Respons fra $metode feilet med status=${respons.status} melding=${respons.melding}",
                null,
                uri,
                data,
            )
        }
    }
}
