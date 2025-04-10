package no.nav.tilleggsstonader.klage.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.infrastruktur.config.OppgaveConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.oppgave.OppgaveClient
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.libs.utils.osloNow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.Optional

@Configuration
class OppgaveClientConfig {
    var maxOppgaveId = 0L
    final var journalPostId = 0L
    val oppgavelager = mutableMapOf<Long, Oppgave>()

    @Profile("bruk-sak-oppgave")
    @Bean
    @Primary
    fun oppgaveClientSak(
        @Qualifier("azure") restTemplate: RestTemplate,
    ): OppgaveClient = OppgaveClient(restTemplate, OppgaveConfig(URI.create("http://localhost:8101/test")))

    @Profile("mock-oppgave")
    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient = mockk<OppgaveClient>()

        opprettOppgave(journalføringsoppgaveRequest)

        every { oppgaveClient.finnOppgaveMedId(any()) } answers {
            val oppgaveId = firstArg<Long>()
            oppgavelager[oppgaveId] ?: error("Finner ikke oppgave=$oppgaveId")
        }

        every { oppgaveClient.opprettOppgave(any()) } answers {
            val oppgave = opprettOppgave(firstArg<OpprettOppgaveRequest>())
            oppgave.id
        }

        val mapper =
            listOf(
                MappeDto(MAPPE_ID_PÅ_VENT, OppgaveMappe.PÅ_VENT.navn.first(), "4462"),
                MappeDto(MAPPE_ID_KLAR, OppgaveMappe.KLAR.navn.first(), "4462"),
            )
        every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(mapper.size, mapper)

        every { oppgaveClient.ferdigstillOppgave(any()) } answers {
            val oppgave = oppgavelager.getValue(firstArg())
            if (oppgave.status == StatusEnum.FERDIGSTILT) {
                error("Allerede ferdigstilt")
            }
            val oppdatertOppgave =
                oppgave.copy(
                    versjon = oppgave.versjon!! + 1,
                    status = StatusEnum.FERDIGSTILT,
                    ferdigstiltTidspunkt = osloNow().format(DateTimeFormatter.ISO_DATE_TIME),
                )
            oppgavelager[oppgave.id] = oppdatertOppgave
        }

        every { oppgaveClient.oppdaterOppgave(any()) } answers {
            val oppdaterOppgave =
                firstArg<Oppgave>().let {
                    val eksisterendeOppgave = oppgavelager[it.id]!!
                    val versjon = it.versjon!!
                    feilHvis(versjon != eksisterendeOppgave.versjon, HttpStatus.CONFLICT) {
                        "Oppgaven har endret seg siden du sist hentet oppgaver. versjon=$versjon (${eksisterendeOppgave.versjon}) " +
                            "For å kunne gjøre endringer må du hente oppgaver på nytt."
                    }
                    eksisterendeOppgave.copy(
                        versjon = versjon + 1,
                        beskrivelse = it.beskrivelse ?: eksisterendeOppgave.beskrivelse,
                        tilordnetRessurs =
                            (
                                it.tilordnetRessurs
                                    ?: eksisterendeOppgave.tilordnetRessurs
                            )?.takeIf { it.isNotBlank() },
                        mappeId = it.mappeId ?: eksisterendeOppgave.mappeId,
                        fristFerdigstillelse = it.fristFerdigstillelse ?: eksisterendeOppgave.fristFerdigstillelse,
                    )
                }

            oppgavelager[oppdaterOppgave.id] = oppdaterOppgave // Forenklet, dette er ikke det som skje ri integrasjoner
            oppdaterOppgave.id
        }

        return oppgaveClient
    }

    private fun opprettOppgave(oppgaveDto: OpprettOppgaveRequest): Oppgave {
        val oppgave =
            Oppgave(
                id = ++maxOppgaveId,
                versjon = 1,
                status = StatusEnum.OPPRETTET,
                identer = oppgaveDto.ident!!.let { listOf(OppgaveIdentV2(it.ident!!, it.gruppe!!)) },
                tildeltEnhetsnr = oppgaveDto.enhetsnummer,
                saksreferanse = null,
                journalpostId = oppgaveDto.journalpostId,
                tema = oppgaveDto.tema,
                oppgavetype = oppgaveDto.oppgavetype.value,
                behandlingstema = oppgaveDto.behandlingstema,
                tilordnetRessurs = oppgaveDto.tilordnetRessurs,
                fristFerdigstillelse = oppgaveDto.fristFerdigstillelse,
                aktivDato = oppgaveDto.aktivFra,
                beskrivelse = oppgaveDto.beskrivelse,
                prioritet = oppgaveDto.prioritet,
                behandlingstype = oppgaveDto.behandlingstype,
                behandlesAvApplikasjon = oppgaveDto.behandlesAvApplikasjon,
                mappeId = oppgaveDto.mappeId?.let { Optional.of(it) },
                opprettetTidspunkt = osloNow().format(DateTimeFormatter.ISO_DATE_TIME),
            )
        oppgavelager[oppgave.id] = oppgave
        return oppgave
    }

    private val journalføringsoppgaveRequest =
        OpprettOppgaveRequest(
            tema = Tema.TSO,
            oppgavetype = Oppgavetype.Journalføring,
            fristFerdigstillelse = osloDateNow().plusDays(14),
            beskrivelse = "Dummy søknad",
            behandlingstema = "ab0300",
            enhetsnummer = "",
            ident = OppgaveIdentV2(ident = "12345678910", gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            journalpostId = (++journalPostId).toString(),
        )

    companion object {
        const val MAPPE_ID_PÅ_VENT = 10L
        const val MAPPE_ID_KLAR = 20L
    }
}
