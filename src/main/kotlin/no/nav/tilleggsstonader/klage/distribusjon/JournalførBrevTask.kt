package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.brev.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.tilleggsstonader.klage.brev.domain.Brev
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalpost
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.distribusjon.JournalføringUtil.mapAvsenderMottaker
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.logger
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførBrevTask.TYPE,
    beskrivelse = "Journalfør brev etter klagebehandling",
)
class JournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandler = task.metadata[SAKSBEHANDLER_METADATA_KEY].toString()

        val brev = brevService.hentBrev(behandlingId)

        val mottakere = brev.mottakere ?: error("Mangler mottakere på brev for behandling=$behandlingId")
        validerMinimumEnMottaker(mottakere)
        journalførBrevmottakere(brev, mottakere, saksbehandler)
    }

    private fun journalførBrevmottakere(
        brev: Brev,
        mottakere: Brevmottakere,
        saksbehandler: String,
    ) {
        val behandlingId = brev.behandlingId
        val avsenderMottakere = mapAvsenderMottaker(mottakere)
        val journalposter = brev.mottakereJournalposter?.journalposter ?: emptyList()
        val brevPdf = brev.brevPdf()

        avsenderMottakere.foldIndexed(journalposter) { index, acc, avsenderMottaker ->
            if (acc.none { it.ident == avsenderMottaker.id }) {
                val journalpostId =
                    distribusjonService.journalførVedtaksbrev(behandlingId, brevPdf, saksbehandler, index, avsenderMottaker)
                val resultat =
                    BrevmottakereJournalpost(
                        ident = avsenderMottaker.id ?: error("Mangler id for mottaker=$avsenderMottaker"),
                        journalpostId = journalpostId,
                    )
                val nyeMottakere = acc + resultat
                brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeMottakere))
                nyeMottakere
            } else {
                acc
            }
        }
    }

    override fun onCompletion(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.resultat == BehandlingResultat.IKKE_MEDHOLD) {
            opprettSendTilKabalTask(task)
        } else {
            logger.info("Skal ikke sende til kabal siden formkrav ikke er oppfylt eller saksbehandler har gitt medhold")
        }
        opprettDistribuerBrevTask(task)
    }

    private fun opprettDistribuerBrevTask(task: Task) {
        val sendTilKabalTask =
            Task(
                type = DistribuerBrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            )
        taskService.save(sendTilKabalTask)
    }

    private fun opprettSendTilKabalTask(task: Task) {
        val sendTilKabalTask =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            )
        taskService.save(sendTilKabalTask)
    }

    companion object {
        const val TYPE = "journalførBrevTask"
    }
}
