package no.nav.tilleggsstonader.klage.vurdering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.brev.BrevRepository
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.dto.tilDto
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class VurderingServiceTest {

    val vurderingRepository = mockk<VurderingRepository>()
    val stegService = mockk<StegService>()
    val brevRepository = mockk<BrevRepository>()
    val vurderingService = VurderingService(vurderingRepository, stegService, brevRepository)

    val omgjørVedtakVurdering = vurdering(
        behandlingId = UUID.randomUUID(),
        vedtak = Vedtak.OMGJØR_VEDTAK,
        hjemmel = null,
        årsak = Årsak.FEIL_I_LOVANDVENDELSE,
        begrunnelseOmgjøring = "begrunnelse",
    )

    val opprettholdVedtakVurdering = vurdering(
        behandlingId = UUID.randomUUID(),
        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        hjemmel = Hjemmel.ARBML_13,
    )

    @BeforeEach
    fun setup() {
        every { vurderingRepository.findByIdOrNull(any()) } returns omgjørVedtakVurdering
        every { vurderingRepository.update(any()) } answers { firstArg() }
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal slette brev ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDto())
        verify(exactly = 1) { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal ikke slette brev ved opprettholdelse`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDto())
        verify(exactly = 0) { brevRepository.deleteById(any()) }
    }

    @Test
    fun `skal oppdatere steg ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDto())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }

    @Test
    fun `skal oppdatere steg ved opprettholdelse av klage`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDto())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }
}
