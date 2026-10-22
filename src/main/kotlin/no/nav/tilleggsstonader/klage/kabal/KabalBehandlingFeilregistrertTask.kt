package no.nav.tilleggsstonader.klage.kabal

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgaveForKlagehendelseTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KabalBehandlingFeilregistrertTask.TYPE,
    beskrivelse = "Håndter feilregistrert klage fra kabal",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class KabalBehandlingFeilregistrertTask(
    private val stegService: StegService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)

        taskService.save(lagOpprettOppgaveTask(behandlingId))

        stegService.oppdaterSteg(
            behandlingId = behandlingId,
            nåværendeSteg = StegType.KABAL_VENTER_SVAR,
            nesteSteg = StegType.BEHANDLING_FERDIGSTILT,
        )
    }

    private fun lagOpprettOppgaveTask(behandlingId: BehandlingId): Task {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val årsakFeilregistrert =
            behandlingService
                .hentKlageresultatDto(behandlingId)
                .single()
                .årsakFeilregistrert ?: error("Fant ikke årsak for feilregistrering")

        return OpprettOppgaveForKlagehendelseTask.opprettTask(
            OpprettOppgavePayload(
                klagebehandlingEksternId = behandling.eksternBehandlingId,
                oppgaveTekst = lagOppgavebeskrivelse(årsakFeilregistrert),
                fagsystem = fagsak.fagsystem,
                klageinstansUtfall = null,
                behandlingstype = Behandlingstype.Klage.value,
            ),
        )
    }

    private fun lagOppgavebeskrivelse(årsakFeilregistrert: String) =
        "Klagebehandlingen er sendt tilbake fra KA med status feilregistrert.\n\nBegrunnelse fra KA: \"$årsakFeilregistrert\""

    companion object {
        const val TYPE = "BehandlingFeilregistrert"

        fun opprettTask(behandlingId: BehandlingId): Task = Task(TYPE, behandlingId.toString())
    }
}
