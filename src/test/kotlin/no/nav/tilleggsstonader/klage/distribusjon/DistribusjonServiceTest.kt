package no.nav.tilleggsstonader.klage.distribusjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DistribusjonServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val tilleggsstønaderIntegrasjonerClient = mockk<TilleggsstønaderIntegrasjonerClient>()

    val distribusjonService = DistribusjonService(tilleggsstønaderIntegrasjonerClient, fagsakService, behandlingService)

    val ident = "1"
    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent(ident)))
    val behandlendeEnhet = "enhet"
    val behandling =
        behandling(fagsak = fagsak, behandlendeEnhet = behandlendeEnhet, resultat = BehandlingResultat.IKKE_MEDHOLD)

    val journalpostSlot = slot<ArkiverDokumentRequest>()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { behandlingService.hentBehandling(any()) } returns behandling
        every {
            tilleggsstønaderIntegrasjonerClient.arkiverDokument(
                capture(journalpostSlot),
                any(),
            )
        } returns ArkiverDokumentResponse("journalpostId", false)
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    fun journalførBrev() {
        val mottaker = AvsenderMottaker(null, null, "navn")
        distribusjonService.journalførBrev(behandling.id, "123".toByteArray(), "saksbehandler", 0, mottaker)

        assertThat(journalpostSlot.captured.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(journalpostSlot.captured.fnr).isEqualTo(ident)
        assertThat(journalpostSlot.captured.journalførendeEnhet).isEqualTo(behandlendeEnhet)
        assertThat(journalpostSlot.captured.forsøkFerdigstill).isEqualTo(true)
        assertThat(journalpostSlot.captured.hoveddokumentvarianter.map { it.dokument }).contains("123".toByteArray())

        assertThat(journalpostSlot.captured.eksternReferanseId).isEqualTo("${behandling.eksternBehandlingId}-0")
    }

    @Test
    fun journalførSaksbehandlingsblankett() {
        distribusjonService.journalførSaksbehandlingsblankett(behandling.id, "pdf".toByteArray(), "saksbehandler")

        assertThat(journalpostSlot.captured.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(journalpostSlot.captured.fnr).isEqualTo(ident)
        assertThat(journalpostSlot.captured.journalførendeEnhet).isEqualTo(behandlendeEnhet)
        assertThat(journalpostSlot.captured.forsøkFerdigstill).isEqualTo(true)
        assertThat(journalpostSlot.captured.hoveddokumentvarianter.map { it.dokument }).contains("pdf".toByteArray())

        assertThat(journalpostSlot.captured.eksternReferanseId).isEqualTo("${behandling.eksternBehandlingId}-blankett")
    }

    @Test
    fun distribuerBrev() {
        val requestSlot = slot<DistribuerJournalpostRequest>()
        val journalpostId = "journalpostId"

        every {
            tilleggsstønaderIntegrasjonerClient.distribuerJournalpost(
                request = capture(requestSlot),
                saksbehandler = any()
            )
        } returns "distribusjonsnummer"

        distribusjonService.distribuerBrev(journalpostId)

        assertThat(requestSlot.captured).isEqualTo(
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "TSO-KLAGE",
                distribusjonstype = Distribusjonstype.ANNET,
            ),
        )
    }
}
