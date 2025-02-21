package no.nav.tilleggsstonader.klage.blankett

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.distribusjon.DistribusjonService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = LagSaksbehandlingsblankettTask.TYPE,
    beskrivelse = "Lager og journalfører blankett",
)
class LagSaksbehandlingsblankettTask(
    private val blankettService: BlankettService,
    private val distribusjonService: DistribusjonService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val blankettPdf = blankettService.lagBlankett(behandlingId)
        val journalpostId =
            distribusjonService.journalførInterntVedtak(
                behandlingId,
                blankettPdf,
                task.metadata.getProperty(SAKSBEHANDLER_METADATA_KEY),
            )

        logger.info("Lagret saksbehandlingsblankett for behandling=$behandlingId på journapost=$journalpostId")
    }

    companion object {
        const val TYPE = "LagSaksbehandlingsblankett"

        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty(SAKSBEHANDLER_METADATA_KEY, SikkerhetContext.hentSaksbehandler(strict = true))
                    },
            )
    }
}
