package no.nav.tilleggsstonader.klage.vurdering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.brev.BrevRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.dto.tilDomene
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class VurderingServiceTest {
    val vurderingRepository = mockk<VurderingRepository>()
    val stegService = mockk<StegService>()
    val brevRepository = mockk<BrevRepository>()
    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val vurderingService =
        VurderingService(
            vurderingRepository = vurderingRepository,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            stegService = stegService,
            brevRepository = brevRepository,
        )

    val omgjørVedtakVurdering =
        vurdering(
            behandlingId = BehandlingId.random(),
            vedtak = Vedtak.OMGJØR_VEDTAK,
            hjemler = null,
            årsak = Årsak.FEIL_I_LOVANDVENDELSE,
            begrunnelseOmgjøring = "begrunnelse",
        )

    val opprettholdVedtakVurdering =
        vurdering(
            behandlingId = BehandlingId.random(),
            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
            hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
        )

    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV, status = BehandlingStatus.UTREDES)

    @BeforeEach
    fun setup() {
        every { vurderingRepository.findByIdOrNull(any()) } returns omgjørVedtakVurdering
        every { vurderingRepository.update(any()) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsak(any()) } returns fagsak
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal slette brev ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDomene())
        verify(exactly = 1) { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal ikke slette brev ved opprettholdelse`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDomene())
        verify(exactly = 0) { brevRepository.deleteById(any()) }
    }

    @Test
    fun `skal oppdatere steg ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDomene())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }

    @Test
    fun `skal oppdatere steg ved opprettholdelse av klage`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDomene())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }
}
