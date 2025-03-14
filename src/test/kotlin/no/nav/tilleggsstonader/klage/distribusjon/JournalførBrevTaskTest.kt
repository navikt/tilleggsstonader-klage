package no.nav.tilleggsstonader.klage.distribusjon

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.brev.domain.Brev
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerPerson
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalpost
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.brev.domain.MottakerRolle
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Fil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Properties

internal class JournalførBrevTaskTest {
    val behandlingService = mockk<BehandlingService>()
    val taskService = mockk<TaskService>()
    val distribusjonService = mockk<DistribusjonService>()
    val brevService = mockk<BrevService>()

    val journalførBrevTask =
        JournalførBrevTask(
            distribusjonService = distribusjonService,
            taskService = taskService,
            behandlingService = behandlingService,
            brevService = brevService,
        )

    val behandlingId: BehandlingId = BehandlingId.random()
    val journalpostId = "12345678"
    val propertiesMedJournalpostId =
        Properties().apply {
            this["journalpostId"] = journalpostId
        }

    val slotBrevmottakereJournalposter = mutableListOf<BrevmottakereJournalposter>()
    val slotSaveTask = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(behandlingId) } returns Pair("ident", fagsak())
        justRun { brevService.oppdaterMottakerJournalpost(any(), capture(slotBrevmottakereJournalposter)) }
        every { taskService.save(capture(slotSaveTask)) } answers { firstArg() }
        every {
            distribusjonService.journalførVedtaksbrev(any(), any(), any(), any(), any())
        } answers { "journalpostId-${(it.invocation.args[3] as Int)}" }
    }

    @Test
    internal fun `skal ikke opprette sendTilKabalTask hvis behandlingen har annen status enn IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskService.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns
            DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(1)
        assertThat(taskSlots.first().type).isEqualTo(DistribuerBrevTask.TYPE)
    }

    @Test
    internal fun `skal opprette sendTilKabalTask og distribuerBrevTask hvis behandlingsresultatet er IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskService.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(2)
        assertThat(taskSlots.first().type).isEqualTo(SendTilKabalTask.TYPE)
        assertThat(taskSlots.last().type).isEqualTo(DistribuerBrevTask.TYPE)
    }

    @Nested
    inner class JournalførMottakere {
        val mottakerPerson = AvsenderMottaker("1", BrukerIdType.FNR, "1navn")
        val mottakerPerson2 = AvsenderMottaker("2", BrukerIdType.FNR, "2navn")
        val mottakerOrganisasjon = AvsenderMottaker("org1", BrukerIdType.ORGNR, "mottaker")

        val mottakere =
            Brevmottakere(
                listOf(
                    BrevmottakerPerson("1", "1navn", MottakerRolle.BRUKER),
                    BrevmottakerPerson("2", "2navn", MottakerRolle.FULLMEKTIG),
                ),
                listOf(BrevmottakerOrganisasjon("org1", "orgnavn", "mottaker")),
            )

        @Test
        internal fun `skal lagre hver person i listen over mottakere`() {
            mockHentBrev(mottakere = mottakere)

            runTask()

            verifyJournalførtBrev(3)
            verifyOrder {
                distribusjonService.journalførVedtaksbrev(behandlingId, any(), any(), 0, mottakerPerson)
                distribusjonService.journalførVedtaksbrev(behandlingId, any(), any(), 1, mottakerPerson2)
                distribusjonService.journalførVedtaksbrev(behandlingId, any(), any(), 2, mottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter[2].journalposter)
        }

        @Test
        internal fun `skal fortsette fra forrige state`() {
            val journalposter =
                listOf(
                    BrevmottakereJournalpost(mottakerPerson.id!!, "journalpostId-0"),
                    BrevmottakereJournalpost(mottakerPerson2.id!!, "journalpostId-1"),
                )
            mockHentBrev(mottakere = mottakere, BrevmottakereJournalposter(journalposter))

            runTask()

            verifyJournalførtBrev(1)
            verifyOrder {
                distribusjonService.journalførVedtaksbrev(behandlingId, any(), any(), 2, mottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter.single().journalposter)
        }

        private fun verifyJournalførtBrev(antallGanger: Int) {
            verify(exactly = antallGanger) {
                distribusjonService.journalførVedtaksbrev(behandlingId, any(), any(), any(), any())
            }
        }

        private fun validerLagringAvBrevmottakereJournalposter(
            journalposter: List<BrevmottakereJournalpost>,
            mottakere: List<AvsenderMottaker> = listOf(mottakerPerson, mottakerPerson2, mottakerOrganisasjon),
        ) {
            assertThat(journalposter).hasSize(3)
            mottakere.forEachIndexed { index, avsenderMottaker ->
                assertThat(journalposter[index].ident).isEqualTo(avsenderMottaker.id)
                assertThat(journalposter[index].journalpostId).isEqualTo("journalpostId-$index")
            }
        }
    }

    private fun runTask(): Task {
        val task = Task(JournalførBrevTask.TYPE, behandlingId.toString())
        journalførBrevTask.doTask(task)
        return task
    }

    private fun mockHentBrev(
        mottakere: Brevmottakere? = null,
        mottakereJournalpost: BrevmottakereJournalposter? = null,
    ) {
        every { brevService.hentBrev(behandlingId) } returns
            Brev(
                behandlingId = behandlingId,
                saksbehandlerHtml = "",
                mottakere = mottakere,
                mottakereJournalposter = mottakereJournalpost,
                pdf = Fil(byteArrayOf()),
            )
    }
}
