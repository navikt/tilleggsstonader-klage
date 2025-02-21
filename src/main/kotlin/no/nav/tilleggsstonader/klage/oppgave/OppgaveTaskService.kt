package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class OppgaveTaskService(
    private val taskService: TaskService,
) {
    fun opprettBehandleSakOppgave(behandlingId: BehandlingId) {
        val behandleSakOppgaveTask =
            Task(
                type = OpprettBehandleSakOppgaveTask.TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                    },
            )
        taskService.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandlingId: BehandlingId) {
        val ferdigstillbehandlesakOppgave =
            Task(
                type = OpprettFerdigstillOppgaveTask.TYPE,
                payload = behandlingId.toString(),
            )
        taskService.save(ferdigstillbehandlesakOppgave)
    }
}
