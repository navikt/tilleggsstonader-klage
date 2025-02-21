package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettFerdigstillOppgaveTask.TYPE,
    beskrivelse = "Ferdigstill oppgave knyttet til behandling",
)
class OpprettFerdigstillOppgaveTask(
    private val oppgaveClient: OppgaveClient,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        oppgaveClient.ferdigstillOppgave(behandleSakOppgave.oppgaveId)
    }

    companion object {
        const val TYPE = "ferdigstillOppgave"
    }
}
