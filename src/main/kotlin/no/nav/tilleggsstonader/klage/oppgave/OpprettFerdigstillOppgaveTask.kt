package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettFerdigstillOppgaveTask.TYPE,
    beskrivelse = "Ferdigstill oppgave knyttet til behandling",
)
class OpprettFerdigstillOppgaveTask(
    private val oppgaveClient: OppgaveClient,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        oppgaveClient.ferdigstillOppgave(
            oppgaveId = behandleSakOppgave.oppgaveId,
            endretAvEnhetsnr = fagsak.stønadstype.behandlendeEnhet().enhetsnr,
        )
    }

    companion object {
        const val TYPE = "ferdigstillOppgave"
    }
}
