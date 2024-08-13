package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class OpprettBehandlingServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal kunne opprette ny klagebehandling selv om en behandling under arbeid allerede eksisterer på samme  fagsak`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        assertThat(behandlingService.hentBehandling(behandling.id).status).isEqualTo(BehandlingStatus.OPPRETTET)

        val nyBehandling = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak))

        assertThat(behandlingService.hentBehandling(nyBehandling).status).isEqualTo(BehandlingStatus.OPPRETTET)
    }

    @Test
    internal fun `skal kunne opprette ny klagebehandling dersom en behandling på samme fagsak venter på resultat fra kabal`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak, status = BehandlingStatus.VENTER, steg = StegType.OVERFØRING_TIL_KABAL)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        val førstegangsbehandling = behandlingService.hentBehandling(behandling.id)
        assertThat(førstegangsbehandling.status).isEqualTo(BehandlingStatus.VENTER)

        val nyBehandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak))

        val andregangsbehandling = behandlingService.hentBehandling(nyBehandlingId)
        assertThat(andregangsbehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(andregangsbehandling.eksternBehandlingId).isNotEqualTo(førstegangsbehandling.eksternBehandlingId)
    }

    @Test
    internal fun `skal ikke kunne opprette klage med krav mottatt frem i tid`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val request = opprettKlagebehandlingRequest(fagsak, LocalDate.now().plusDays(1))

        val feil = assertThrows<Feil> { opprettBehandlingService.opprettBehandling(request) }

        assertThat(feil.frontendFeilmelding).contains("Kan ikke opprette klage med krav mottatt frem i tid for eksternFagsakId")
    }

    private fun opprettKlagebehandlingRequest(
        fagsak: Fagsak,
        klageMottatt: LocalDate = LocalDate.now().minusDays(1),
    ) =
        OpprettKlagebehandlingRequest(
            ident = "1234",
            stønadstype = fagsak.stønadstype,
            eksternFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem,
            klageMottatt = klageMottatt,
            behandlendeEnhet = "4462",
        )
}
