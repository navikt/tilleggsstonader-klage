package no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.domain.AuditLogger
import no.nav.tilleggsstonader.klage.felles.domain.BehandlerRolle
import no.nav.tilleggsstonader.klage.infrastruktur.config.RolleConfigTestUtil
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstonaderSakClient
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class TilgangServiceTest {
    private val tilleggsstonaderSakClient = mockk<TilleggsstonaderSakClient>()
    private val rolleConfig = RolleConfigTestUtil.rolleConfig
    private val cacheManager = ConcurrentMapCacheManager()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()

    private val tilgangService =
        TilgangService(
            tilleggsstonaderSakClient = tilleggsstonaderSakClient,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = auditLogger,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
        )

    private val fagsakEf = fagsak()
    private val behandlingEf = behandling(fagsakEf)

    @BeforeEach
    internal fun setUp() {
        mockFagsakOgBehandling(fagsakEf, behandlingEf)
    }

    private fun mockFagsakOgBehandling(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class TilgangGittRolle {
        @Test
        internal fun `saksbehandler har tilgang som veileder og saksbehandler, men ikke beslutter`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ts.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isFalse
            }
        }

        @Test
        internal fun `beslutter har tilgang som saksbehandler, beslutter og veileder`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ts.beslutter)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isTrue
            }
        }
    }
}
