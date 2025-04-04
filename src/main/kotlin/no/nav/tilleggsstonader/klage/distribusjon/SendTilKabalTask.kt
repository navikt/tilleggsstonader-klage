package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata
import no.nav.tilleggsstonader.klage.kabal.KabalService
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendTilKabalTask.TYPE,
    beskrivelse = "Send klage til kabal",
)
class SendTilKabalTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val kabalService: KabalService,
    private val vurderingService: VurderingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandlerIdent = task.metadata[TaskMetadata.SAKSBEHANDLER_METADATA_KEY].toString()
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val vurdering =
            vurderingService.hentVurdering(behandlingId) ?: error("Mangler vurdering på klagen - kan ikke oversendes til kabal")
        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerIdent)
    }

    companion object {
        const val TYPE = "sendTilKabalTask"
    }
}
