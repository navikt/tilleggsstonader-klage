package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.prosessering.AsyncTaskStep
import no.nav.tilleggsstonader.prosessering.TaskStepBeskrivelse
import no.nav.tilleggsstonader.prosessering.domene.Task
import no.nav.tilleggsstonader.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingFeilregistrertTask.TYPE,
    beskrivelse = "Håndter feilregistret klage fra kabal",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class BehandlingFeilregistrertTask(
    private val stegService: StegService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        taskService.save(lagOpprettOppgaveTask(behandlingId))

        stegService.oppdaterSteg(
            behandlingId,
            StegType.KABAL_VENTER_SVAR,
            StegType.BEHANDLING_FERDIGSTILT,
        )
    }

    private fun lagOpprettOppgaveTask(behandlingId: UUID): Task {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val årsakFeilregistrert = behandlingService.hentKlageresultatDto(behandlingId)
            .single().årsakFeilregistrert ?: error("Fant ikke årsak for feilregistrering")

        return OpprettKabalEventOppgaveTask.opprettTask(
            OpprettOppgavePayload(
                klagebehandlingEksternId = behandling.eksternBehandlingId,
                oppgaveTekst = lagOppgavebeskrivelse(årsakFeilregistrert),
                fagsystem = fagsak.fagsystem,
                klageinstansUtfall = null,
                behandlingstype = Behandlingstema.Klage.value,
            ),
        )
    }

    private fun lagOppgavebeskrivelse(årsakFeilregistrert: String) =
        "Klagebehandlingen er sendt tilbake fra KA med status feilregistrert.\n\nBegrunnelse fra KA: \"$årsakFeilregistrert\""

    companion object {

        const val TYPE = "BehandlingFeilregistrert"

        fun opprettTask(behandlingId: UUID): Task =
            Task(TYPE, behandlingId.toString())
    }
}
