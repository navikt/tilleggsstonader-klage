package no.nav.tilleggsstonader.klage.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

internal class OpprettRevurderingServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    val service = OpprettRevurderingService(behandlingService, fagsystemVedtakService)

    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandlingId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        every { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) } returns
            KanOppretteRevurderingResponse(true, null)
    }

    @Test
    internal fun `kan opprette revurdering for vedtak`() {
        every { behandlingService.hentBehandling(behandlingId) } returns
            behandling(fagsak = fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, null))

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isTrue
        verify { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }

    @EnumSource(
        value = PåklagetVedtakstype::class,
        names = ["VEDTAK"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    @ParameterizedTest
    internal fun `skal ikke opprette revurdering for andre påklagede vedtakstyper enn vedtak`(påklagetVedtakstype: PåklagetVedtakstype) {
        every { behandlingService.hentBehandling(behandlingId) } returns
            behandling(fagsak = fagsak, påklagetVedtak = PåklagetVedtak(påklagetVedtakstype))

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isFalse
        verify(exactly = 0) { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }
}
