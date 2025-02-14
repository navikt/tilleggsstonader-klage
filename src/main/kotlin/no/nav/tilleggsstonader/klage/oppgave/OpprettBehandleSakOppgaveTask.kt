package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettBehandleSakOppgaveTask.TYPE,
    beskrivelse = "Opprett behandle sak oppgave",
)
class OpprettBehandleSakOppgaveTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val oppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = fagsak.hentAktivIdent(), gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksreferanse = fagsak.eksternId, // fagsakId fra fagsystem
                tema = fagsak.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.BehandleSak,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = "Klagebehandling i ny løsning",
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstype = Behandlingstype.Klage.value,
                behandlesAvApplikasjon = "tilleggsstonader-klage",
                tilordnetRessurs = task.metadata.getProperty(SAKSBEHANDLER_METADATA_KEY),
                behandlingstema = fagsak.stønadstype.tilBehandlingstema().value,
                mappeId = oppgaveService.finnMappe(behandling.behandlendeEnhet, OppgaveMappe.KLAR),
            )

        val oppgaveId = oppgaveService.opprettOppgave(opprettOppgaveRequest = oppgaveRequest)
        behandleSakOppgaveRepository.insert(
            BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = oppgaveId),
        )
    }

    companion object {
        const val TYPE = "opprettBehandleSakoppgave"
    }
}
