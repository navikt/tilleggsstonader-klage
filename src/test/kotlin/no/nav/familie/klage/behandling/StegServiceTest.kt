package no.nav.familie.klage.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.infrastruktur.config.FagsystemRolleConfig
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class StegServiceTest {

    val behandlingRepository = mockk<BehandlingRepository>()
    val behandlingshistorikkService = mockk<BehandlingshistorikkService>()

    val veilederRolle = "veilederRolle"
    val stegService = StegService(
        behandlingRepository,
        behandlingshistorikkService,
        RolleConfig(
            FagsystemRolleConfig("", "", ""),
            FagsystemRolleConfig("", "", veilederRolle),
            FagsystemRolleConfig("", "", "")
        )
    )
    val behandlingId = UUID.randomUUID()
    val behandling = behandling(id = behandlingId)

    val historikkSlot = mutableListOf<StegType>()
    val stegSlot = slot<StegType>()
    val statusSlot = slot<BehandlingStatus>()

    @BeforeEach
    internal fun setUp() {
        historikkSlot.clear()
        stegSlot.clear()
        statusSlot.clear()
        mockkObject(SikkerhetContext)

        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        every { SikkerhetContext.harTilgangTilGittRolle(any(), any()) } returns true

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every { behandlingRepository.updateSteg(behandlingId, capture(stegSlot)) } just Runs
        every { behandlingRepository.updateStatus(behandlingId, capture(statusSlot)) } just Runs
        every { behandlingshistorikkService.opprettBehandlingshistorikk(any(), capture(historikkSlot)) } returns mockk()
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun oppdaterSteg() {
        assertThat(behandling.steg).isEqualTo(StegType.FORMKRAV)

        val nesteSteg = StegType.VURDERING
        stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, nesteSteg)

        assertThat(stegSlot.captured).isEqualTo(nesteSteg)
        assertThat(statusSlot.captured).isEqualTo(nesteSteg.gjelderStatus)
        assertThat(historikkSlot.single()).isEqualTo(behandling.steg)
        verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any()) }
    }

    @Test
    internal fun `skal legge inn overføring til kabal i historikken når neste steg er venter på svar då det er steg vi hopper over men for historikk i frontend`() {
        stegService.oppdaterSteg(behandlingId, behandling.steg, StegType.KABAL_VENTER_SVAR)

        verifyOrder {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, behandling.steg)
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.OVERFØRING_TIL_KABAL)
        }
    }

    @Test
    internal fun `skal legge inn ferdigstill i historikken når neste steg er ferdigstill`() {
        stegService.oppdaterSteg(behandlingId, behandling.steg, StegType.BEHANDLING_FERDIGSTILT)

        verifyOrder {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, behandling.steg)
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.BEHANDLING_FERDIGSTILT)
        }
    }

    @Test
    internal fun `skal oppdatere steg hvis den allerede er i det samme steget`() {
        stegService.oppdaterSteg(behandlingId, behandling.steg, behandling.steg)

        verify(exactly = 1) { behandlingRepository.updateSteg(any(), any()) }
        verify(exactly = 1) { behandlingRepository.updateStatus(any(), any()) }
        verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any()) }
    }

    @Test
    fun `skal feile hvis saksbehandler mangler rolle`() {
        unmockkObject(SikkerhetContext)
        testWithBrukerContext(groups = listOf(veilederRolle)) {
            val feil = assertThrows<Feil> { stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.VURDERING) }

            assertThat(feil.frontendFeilmelding).contains("Saksbehandler har ikke tilgang til å oppdatere behandlingssteg")
        }
    }

    @Test
    fun `skal feile hvis behandling er låst`() {
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling(status = BehandlingStatus.FERDIGSTILT)

        val feil = assertThrows<Feil> {
            stegService.oppdaterSteg(
                UUID.randomUUID(),
                StegType.VURDERING,
                StegType.BREV
            )
        }
        assertThat(feil.frontendFeilmelding).contains("Behandlingen er låst for videre behandling")
    }
}
