package no.nav.tilleggsstonader.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.data.repository.findByIdOrNull

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

    @Nested
    inner class BehandleSakOppgaveGjelderAktivitetspenger {
        private val oppgaveId = 1L

        @Test
        fun `skal returnere true når behandle sak-oppgaven har behandlingstype aktivitetspenger klage`() {
            mockBehandleSakOppgave(Behandlingstype.AktivitetspengerKlage.value)

            assertThat(oppgaveService.behandleSakOppgaveGjelderAktivitetspenger(behandlingId)).isTrue()
        }

        @Test
        fun `skal returnere false når behandle sak-oppgaven har behandlingstype klage`() {
            mockBehandleSakOppgave(Behandlingstype.Klage.value)

            assertThat(oppgaveService.behandleSakOppgaveGjelderAktivitetspenger(behandlingId)).isFalse()
        }

        @Test
        fun `skal returnere false når behandlingen mangler behandle sak-oppgave`() {
            every { behandleSakOppgaveRepository.findByIdOrNull(behandlingId) } returns null

            assertThat(oppgaveService.behandleSakOppgaveGjelderAktivitetspenger(behandlingId)).isFalse()
            verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(any()) }
        }

        private fun mockBehandleSakOppgave(behandlingstype: String) {
            every { behandleSakOppgaveRepository.findByIdOrNull(behandlingId) } returns
                BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId)
            every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns
                Oppgave(id = oppgaveId, versjon = 1, behandlingstype = behandlingstype)
        }
    }
}
