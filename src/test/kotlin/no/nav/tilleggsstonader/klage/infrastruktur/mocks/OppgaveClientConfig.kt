package no.nav.tilleggsstonader.klage.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.oppgave.OppgaveClient
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.*
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.libs.utils.osloNow
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import java.time.format.DateTimeFormatter
import java.util.*

@Configuration
@Profile("mock-oppgave")
class OppgaveClientConfig {

    var maxOppgaveId = 0L
    final var journalPostId = 0L
    val oppgavelager = mutableMapOf<Long, Oppgave>()

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

        every { oppgaveClient.ferdigstillOppgave(any()) } answers {
            val oppgave = oppgavelager.getValue(firstArg())
            if (oppgave.status == StatusEnum.FERDIGSTILT) {
                error("Allerede ferdigstilt")
            }
            val oppdatertOppgave = oppgave.copy(
                versjon = oppgave.versjon!! + 1,
                status = StatusEnum.FERDIGSTILT,
                ferdigstiltTidspunkt = osloNow().format(DateTimeFormatter.ISO_DATE_TIME),
            )
            oppgavelager[oppgave.id] = oppdatertOppgave
        }

        every { oppgaveClient.oppdaterOppgave(any()) } answers {
            val oppdaterOppgave = firstArg<Oppgave>().let {
                val eksisterendeOppgave = oppgavelager[it.id]!!
                val versjon = it.versjon!!
                feilHvis(versjon != eksisterendeOppgave.versjon, HttpStatus.CONFLICT) {
                    "Oppgaven har endret seg siden du sist hentet oppgaver. versjon=$versjon (${eksisterendeOppgave.versjon}) " +
                        "For å kunne gjøre endringer må du hente oppgaver på nytt."
                }
                eksisterendeOppgave.copy(
                    versjon = versjon + 1,
                    beskrivelse = it.beskrivelse ?: eksisterendeOppgave.beskrivelse,
                    tilordnetRessurs = (
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

    private fun opprettOppgave(
        oppgaveDto: OpprettOppgaveRequest,
    ): Oppgave {
        val oppgave = Oppgave(
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



    private val journalføringsoppgaveRequest = OpprettOppgaveRequest(
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
        const val MAPPE_ID_PÅ_VENT = 10
    }
}
