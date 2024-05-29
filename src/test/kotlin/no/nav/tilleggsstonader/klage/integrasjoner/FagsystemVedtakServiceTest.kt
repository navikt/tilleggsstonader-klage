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

    private val efSakClient = mockk<FamilieEFSakClient>()
    private val fagsakService = mockk<FagsakService>()
    private val service = FagsystemVedtakService(
        familieEFSakClient = efSakClient,
        fagsakService = fagsakService,
    )

    private val fagsakEF = fagsak(stønadstype = Stønadstype.BARNETILSYN)

    private val behandlingEF = behandling(fagsakEF)

    private val påklagetBehandlingId = "påklagetBehandlingId"

    private val vedtak = fagsystemVedtak(påklagetBehandlingId)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandlingEF.id) } returns fagsakEF

        every { efSakClient.hentVedtak(fagsakEF.eksternId) } returns listOf(vedtak)
    }

    @Nested
    inner class HentFagsystemVedtak {

        @Test
        internal fun `skal kalle på ef-klient for ef-behandling`() {
            service.hentFagsystemVedtak(behandlingEF.id)

            verify { efSakClient.hentVedtak(any()) }
        }
    }

    @Nested
    inner class HentFagsystemVedtakForPåklagetBehandlingId {

        @Test
        internal fun `skal returnere fagsystemVedtak`() {
            val fagsystemVedtak = service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, påklagetBehandlingId)

            assertThat(fagsystemVedtak).isNotNull
            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis fagsystemVedtak ikke finnes med forventet eksternBehandlingId`() {
            assertThatThrownBy {
                service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, "finnes ikke")
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }
}
