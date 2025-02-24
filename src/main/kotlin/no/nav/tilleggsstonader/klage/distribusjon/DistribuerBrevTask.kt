package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.brev.domain.Brev
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalpost
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerBrevTask.TYPE,
    beskrivelse = "Distribuer brev etter klagebehandling",
)
class DistribuerBrevTask(
    private val brevService: BrevService,
    private val distribusjonService: DistribusjonService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val brev = brevService.hentBrev(behandlingId)
        val journalposter = mottakereJournalpost(brev)

        validerHarJournalposter(behandlingId, journalposter)

        val distribueringer =
            journalposter.fold(journalposter) { acc, journalpost ->
                distribuerOgLagreJournalposter(behandlingId, acc, journalpost)
            }

        feilHvis(distribueringer.any { it.distribusjonId == null }) {
            "Mangler distribusjonId for journalpost"
        }
    }

    private fun distribuerOgLagreJournalposter(
        behandlingId: BehandlingId,
        acc: List<BrevmottakereJournalpost>,
        journalpost: BrevmottakereJournalpost,
    ): List<BrevmottakereJournalpost> =
        if (journalpost.distribusjonId == null) {
            val distribusjonId = distribusjonService.distribuerBrev(journalpost.journalpostId)
            val nyeJournalposter =
                acc.map {
                    if (it.journalpostId == journalpost.journalpostId) {
                        it.copy(distribusjonId = distribusjonId)
                    } else {
                        it
                    }
                }
            brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeJournalposter))
            nyeJournalposter
        } else {
            acc
        }

    private fun validerHarJournalposter(
        behandlingId: BehandlingId,
        journalposter: List<BrevmottakereJournalpost>,
    ) {
        feilHvis(journalposter.isEmpty()) {
            "Mangler journalposter for behandling=$behandlingId"
        }
    }

    private fun mottakereJournalpost(brev: Brev): List<BrevmottakereJournalpost> =
        brev.mottakereJournalposter?.journalposter?.takeIf { it.isNotEmpty() }
            ?: error("Mangler journalposter koblet til brev=${brev.behandlingId}")

    companion object {
        const val TYPE = "distribuerBrevTask"
    }
}
