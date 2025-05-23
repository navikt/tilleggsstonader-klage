package no.nav.tilleggsstonader.klage.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.distribusjon.DistribusjonService
import no.nav.tilleggsstonader.klage.distribusjon.JournalførBrevTask
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.kabal.KabalService
import no.nav.tilleggsstonader.klage.oppgave.OppgaveTaskService
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.Opprettet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FerdigstillBehandlingServiceTest {
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val distribusjonService = mockk<DistribusjonService>()
    val kabalService = mockk<KabalService>()
    val vurderingService = mockk<VurderingService>()

    val formService = mockk<FormService>()
    val stegService = mockk<StegService>()
    val taskService = mockk<TaskService>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()
    val brevService = mockk<BrevService>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    val behandlingshistorikkService = mockk<BehandlingshistorikkService>()

    val ferdigstillBehandlingService =
        FerdigstillBehandlingService(
            behandlingService = behandlingService,
            vurderingService = vurderingService,
            formService = formService,
            stegService = stegService,
            taskService = taskService,
            oppgaveTaskService = oppgaveTaskService,
            brevService = brevService,
            fagsystemVedtakService = fagsystemVedtakService,
            behandlingshistorikkService = behandlingshistorikkService,
        )
    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV, status = BehandlingStatus.UTREDES)
    val vurdering = vurdering(behandlingId = behandling.id)
    val journalpostId = "1234"
    val brevDistribusjonId = "9876"

    val saveTaskSlot = mutableListOf<Task>()

    val stegSlot = slot<StegType>()
    val behandlingsresultatSlot = slot<BehandlingResultat>()
    val fagsystemRevurderingSlot = mutableListOf<FagsystemRevurdering?>()

    @BeforeEach
    internal fun setUp() {
        fagsystemRevurderingSlot.clear()
        BrukerContextUtil.mockBrukerContext("halla")
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { distribusjonService.journalførVedtaksbrev(any(), any(), any(), any(), any()) } returns journalpostId
        every { distribusjonService.distribuerBrev(any()) } returns brevDistribusjonId
        every { vurderingService.hentVurdering(any()) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any(), any()) } just Runs
        justRun { stegService.oppdaterSteg(any(), any(), capture(stegSlot), any()) }
        every { formService.formkravErOppfyltForBehandling(any()) } returns true
        justRun { behandlingService.oppdaterBehandlingMedResultat(any(), capture(behandlingsresultatSlot), null) }
        justRun {
            behandlingService.oppdaterBehandlingMedResultat(
                any(),
                capture(behandlingsresultatSlot),
                captureNullable(fagsystemRevurderingSlot),
            )
        }
        every { taskService.save(capture(saveTaskSlot)) } answers { firstArg() }
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id) } just Runs
        justRun { brevService.lagBrevPdf(any()) }
        every { fagsystemVedtakService.opprettRevurdering(any()) } returns OpprettRevurderingResponse(Opprettet("opprettetId"))
        justRun { behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandling.id) }
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling, ikke medhold`() {
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
        assertThat(fagsystemRevurderingSlot.single()).isNull()
        assertThat(stegSlot.captured).isEqualTo(StegType.KABAL_VENTER_SVAR)

        verify(exactly = 4) { taskService.save(any()) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(
            JournalførBrevTask.TYPE,
            LagSaksbehandlingsblankettTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
        )
        verify { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis formkrav ikke er oppfylt`() {
        every { formService.formkravErOppfyltForBehandling(any()) } returns false

        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)
        assertThat(fagsystemRevurderingSlot.single()).isNull()

        verify { taskService.save(any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis klage tas til følge`() {
        every { vurderingService.hentVurdering(any()) } returns vurdering.copy(vedtak = Vedtak.OMGJØR_VEDTAK)

        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.MEDHOLD)
        assertThat(fagsystemRevurderingSlot.single()).isNull()

        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 0) { fagsystemVedtakService.opprettRevurdering(behandling.id) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(
            LagSaksbehandlingsblankettTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
        )
    }

    @Test
    internal fun `skal feile dersom behandlingen er på feil steg`() {
        listOf(
            StegType.BEHANDLING_FERDIGSTILT,
            StegType.FORMKRAV,
            StegType.OVERFØRING_TIL_KABAL,
            StegType.VURDERING,
        ).forEach { steg ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(steg = steg)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }

    @Test
    internal fun `skal feile dersom behandlingen har feil status`() {
        listOf(
            BehandlingStatus.FERDIGSTILT,
            BehandlingStatus.VENTER,
        ).forEach { status ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(status = status)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }

    @Test
    internal fun `skal opprette revurdering automatisk påklaget vedtak er vedtak i fagsystemet`() {
        every { vurderingService.hentVurdering(any()) } returns vurdering.copy(vedtak = Vedtak.OMGJØR_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns
            behandling.copy(påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer()))

        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(fagsystemRevurderingSlot.single()).isNotNull
        verify(exactly = 1) { fagsystemVedtakService.opprettRevurdering(behandling.id) }
    }
}
