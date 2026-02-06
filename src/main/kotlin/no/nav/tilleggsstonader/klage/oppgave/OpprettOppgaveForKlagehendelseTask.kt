package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakPersonRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveForKlagehendelseTask.TYPE,
    beskrivelse = "Opprett 'Vurder konsekvens for ytelse'-oppgave for klagehendelse",
)
class OpprettOppgaveForKlagehendelseTask(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personRepository: FagsakPersonRepository,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val opprettOppgavePayload = jsonMapper.readValue<OpprettOppgavePayload>(task.payload)
        val behandling = behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId)
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        val personId =
            fagsakDomain?.fagsakPersonId
                ?: throw Feil(
                    "Feil ved henting av aktiv ident: Finner ikke fagsak for behandling med klagebehandlingEksternId ${opprettOppgavePayload.klagebehandlingEksternId}",
                )

        val aktivIdent = personRepository.hentAktivIdent(personId)
        val prioritet = utledOppgavePrioritet(opprettOppgavePayload.klageinstansUtfall)

        val opprettVurderKonsekvensForYtelseOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksreferanse = fagsakDomain.eksternId,
                tema = fagsakDomain.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = opprettOppgavePayload.oppgaveTekst,
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = opprettOppgavePayload.behandlingstema?.value,
                behandlingstype = opprettOppgavePayload.behandlingstype,
                prioritet = prioritet,
            )

        val oppgaveId = oppgaveService.opprettOppgaveUtenÅLagreIRepository(opprettVurderKonsekvensForYtelseOppgaveRequest)
        logger.info("Oppgave opprettet med id $oppgaveId")
    }

    companion object {
        const val TYPE = "opprettOppgaveForKlagehendelse"

        fun opprettTask(opprettOppgavePayload: OpprettOppgavePayload): Task =
            Task(
                TYPE,
                jsonMapper.writeValueAsString(opprettOppgavePayload),
            )
    }

    private fun utledOppgavePrioritet(klageinstansUtfall: KlageinstansUtfall?): OppgavePrioritet =
        when (klageinstansUtfall) {
            KlageinstansUtfall.OPPHEVET -> OppgavePrioritet.HOY
            else -> {
                OppgavePrioritet.NORM
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
