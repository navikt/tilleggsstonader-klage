package no.nav.tilleggsstonader.klage.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.infrastruktur.config.OppgaveConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
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
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional

class OppgaveTestClient(
    val oppgavelager: MutableMap<Long, Oppgave>,
) {
    fun plukkOppgave(id: Long) {
        oppgavelager[id] =
            oppgavelager.getValue(id).let {
                it.copy(versjon = it.versjon + 1, tilordnetRessurs = SikkerhetContext.hentSaksbehandler())
            }
    }
}

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

    @Bean
    fun oppgaveTestClient(): OppgaveTestClient = OppgaveTestClient(oppgavelager)

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

        every { oppgaveClient.settPåVent(any()) } answers {
            val request = firstArg<SettPåVentRequest>()
            val oppgave = oppgavelager.getValue(request.oppgaveId)
            brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                "Kan ikke sette behandling på vent når man ikke er eier av oppgaven."
            }
            val versjon = oppgave.versjon + 1
            oppgavelager[request.oppgaveId] =
                oppgave.copy(
                    versjon = versjon,
                    beskrivelse = request.kommentar + "\n" + oppgave.beskrivelse,
                    fristFerdigstillelse = request.frist,
                    mappeId = Optional.of(MAPPE_ID_PÅ_VENT),
                    tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                )
            SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
        }

        every { oppgaveClient.oppdaterPåVent(any()) } answers {
            val request = firstArg<OppdaterPåVentRequest>()
            val oppgave = oppgavelager.getValue(request.oppgaveId)
            brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                "Kan ikke oppdatere behandling på vent når man ikke er eier av oppgaven."
            }
            brukerfeilHvis(oppgave.versjon != request.oppgaveVersjon) {
                "Versjon er feil"
            }
            val versjon = oppgave.versjon + 1
            oppgavelager[request.oppgaveId] =
                oppgave.copy(
                    versjon = versjon,
                    beskrivelse = request.kommentar + "\n" + oppgave.beskrivelse,
                    fristFerdigstillelse = request.frist,
                    tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                )
            SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
        }

        every { oppgaveClient.taAvVent(any()) } answers {
            val request = firstArg<TaAvVentRequest>()
            val oppgave = oppgavelager.getValue(request.oppgaveId)
            brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                "Kan ikke ta behandling av vent når man ikke er eier av oppgaven."
            }

            val versjon = oppgave.versjon + 1
            oppgavelager[request.oppgaveId] =
                oppgave.copy(
                    versjon = versjon,
                    beskrivelse = request.kommentar + "\n Tatt av vent\n" + oppgave.beskrivelse,
                    fristFerdigstillelse = LocalDate.now(),
                    tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                    mappeId = Optional.of(MAPPE_ID_KLAR),
                )
            SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
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
