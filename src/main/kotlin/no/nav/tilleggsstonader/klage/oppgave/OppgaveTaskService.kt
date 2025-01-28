package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(
    private val taskService: TaskService,
) {
    fun opprettBehandleSakOppgave(behandlingId: UUID) {
        val behandleSakOppgaveTask =
            Task(
                type = OpprettBehandleSakOppgaveTask.TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
                    },
            )
        taskService.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandlingId: UUID) {
        val ferdigstillbehandlesakOppgave =
            Task(
                type = OpprettFerdigstillOppgaveTask.TYPE,
                payload = behandlingId.toString(),
            )
        taskService.save(ferdigstillbehandlesakOppgave)
    }
}
