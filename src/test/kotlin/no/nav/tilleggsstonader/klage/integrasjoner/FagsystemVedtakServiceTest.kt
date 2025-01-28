package no.nav.tilleggsstonader.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class FagsystemVedtakServiceTest {
    private val tilleggsstonaderSakClient = mockk<TilleggsstonaderSakClient>()
    private val fagsakService = mockk<FagsakService>()
    private val service =
        FagsystemVedtakService(
            tilleggsstonaderSakClient = tilleggsstonaderSakClient,
            fagsakService = fagsakService,
        )

    private val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

    private val behandling = behandling(fagsak)

    private val påklagetBehandlingId = "påklagetBehandlingId"

    private val vedtak = fagsystemVedtak(påklagetBehandlingId)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak

        every { tilleggsstonaderSakClient.hentVedtak(fagsak.eksternId) } returns listOf(vedtak)
    }

    @Nested
    inner class HentFagsystemVedtak {
        @Test
        internal fun `skal kalle på ts-klient for ts-behandling`() {
            service.hentFagsystemVedtak(behandling.id)

            verify { tilleggsstonaderSakClient.hentVedtak(any()) }
        }
    }

    @Nested
    inner class HentFagsystemVedtakForPåklagetBehandlingId {
        @Test
        internal fun `skal returnere fagsystemVedtak`() {
            val fagsystemVedtak = service.hentFagsystemVedtakForPåklagetBehandlingId(behandling.id, påklagetBehandlingId)

            assertThat(fagsystemVedtak).isNotNull
            verify { tilleggsstonaderSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis fagsystemVedtak ikke finnes med forventet eksternBehandlingId`() {
            assertThatThrownBy {
                service.hentFagsystemVedtakForPåklagetBehandlingId(behandling.id, "finnes ikke")
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }
}
