package no.nav.tilleggsstonader.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class OppgaveServiceTest {
    private val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val oppgaveService =
        OppgaveService(
            behandleSakOppgaveRepository,
            oppgaveClient,
            behandlingService,
            cacheManager,
        )

    val behandlingId: BehandlingId = BehandlingId.random()

    @Test
    internal fun `skal ikke oppdatere oppgave om behandling ikke har status opprettet eller utredes`() {
        val behandling = behandling(id = behandlingId, status = BehandlingStatus.FERDIGSTILT)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)

        verify(exactly = 0) { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
        verify(exactly = 0) { oppgaveClient.oppdaterOppgave(any()) }
    }
}
