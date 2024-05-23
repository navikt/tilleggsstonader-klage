package no.nav.tilleggsstonader.klage.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakPersonRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.klage.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.tilleggsstonader.kontrakter.felles.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.prosessering.AsyncTaskStep
import no.nav.tilleggsstonader.prosessering.TaskStepBeskrivelse
import no.nav.tilleggsstonader.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettKabalEventOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave for relevant hendelse fra kabal",
)
class OpprettKabalEventOppgaveTask(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personRepository: FagsakPersonRepository,
    private val oppgaveClient: OppgaveClient,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(task.payload)
        val behandling = behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId)
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        val personId = fagsakDomain?.fagsakPersonId
            ?: throw Feil("Feil ved henting av aktiv ident: Finner ikke fagsak for behandling med klagebehandlingEksternId ${opprettOppgavePayload.klagebehandlingEksternId}")

        val aktivIdent = personRepository.hentAktivIdent(personId)
        val prioritet = utledOppgavePrioritet(opprettOppgavePayload.klageinstansUtfall)

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsakDomain.eksternId,
                tema = fagsakDomain.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = opprettOppgavePayload.oppgaveTekst,
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = opprettOppgavePayload.behandlingstema?.value,
                behandlingstype = opprettOppgavePayload.behandlingstype,
                prioritet = prioritet,
            )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        logger.info("Oppgave opprettet med id $oppgaveId")
    }

    companion object {

        const val TYPE = "opprettOppgaveForKlagehendelse"

        fun opprettTask(opprettOppgavePayload: OpprettOppgavePayload): Task {
            return Task(
                TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload),
            )
        }
    }

    private fun utledOppgavePrioritet(klageinstansUtfall: KlageinstansUtfall?): OppgavePrioritet {
        return when (klageinstansUtfall) {
            KlageinstansUtfall.OPPHEVET -> OppgavePrioritet.HOY
            else -> {
                OppgavePrioritet.NORM
            }
        }
    }
}

data class OpprettOppgavePayload(
    val klagebehandlingEksternId: UUID,
    val oppgaveTekst: String,
    val fagsystem: Fagsystem,
    val klageinstansUtfall: KlageinstansUtfall?,
    val behandlingstema: Behandlingstema? = null,
    val behandlingstype: String? = null,
)
