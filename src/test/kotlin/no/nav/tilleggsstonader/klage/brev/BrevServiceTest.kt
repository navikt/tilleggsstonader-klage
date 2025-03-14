package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.UTEN_VEDTAK
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.formkrav.FormRepository
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.vurdering.VurderingRepository
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevServiceTest : IntegrationTest() {
    @Autowired
    lateinit var brevService: BrevService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var formRepository: FormRepository

    @Autowired
    lateinit var vurderingRepository: VurderingRepository

    private val fagsak = fagsak()
    private val påklagetVedtak = PåklagetVedtak(VEDTAK, påklagetVedtakDetaljer("123"))
    private val behandlingPåklagetVedtak = behandling(fagsak, steg = StegType.BREV, påklagetVedtak = påklagetVedtak)
    private val ferdigstiltBehandling =
        behandling(
            fagsak,
            status = BehandlingStatus.FERDIGSTILT,
            påklagetVedtak = påklagetVedtak,
        )
    private val behandlingUtenPåklagetVedtak =
        behandling(fagsak, steg = StegType.BREV, påklagetVedtak = påklagetVedtak.copy(påklagetVedtakstype = UTEN_VEDTAK))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandlingPåklagetVedtak)
        testoppsettService.lagreBehandling(ferdigstiltBehandling)
        testoppsettService.lagreBehandling(behandlingUtenPåklagetVedtak)

        formRepository.insert(DomainUtil.oppfyltForm(behandlingPåklagetVedtak.id))
        vurderingRepository.insert(DomainUtil.vurdering(behandlingPåklagetVedtak.id))
        formRepository.insert(DomainUtil.oppfyltForm(behandlingUtenPåklagetVedtak.id))
        vurderingRepository.insert(DomainUtil.vurdering(behandlingUtenPåklagetVedtak.id))

        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class LagEllerOppdaterBrev {
        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen er låst`() {
            assertThatThrownBy { brevService.lagBrev(ferdigstiltBehandling.id) }
                .hasMessage("Kan ikke oppdatere brev når behandlingen er låst")
        }

        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen ikke er i brevsteget`() {
            behandlingRepository.update(behandlingPåklagetVedtak.copy(steg = StegType.FORMKRAV))
            assertThatThrownBy { brevService.lagBrev(behandlingPåklagetVedtak.id) }
                .hasMessageContaining("Behandlingen er i feil steg ")
        }

        @Test
        internal fun `skal kunne lage avvisningsbrev når behandlingen har påklaget vedtakstype uten vedtak`() {
            assertThat(brevService.lagBrev(behandlingUtenPåklagetVedtak.id)).isNotNull
        }
    }

    @Nested
    inner class LagBrevSomPdf {
        @Test
        internal fun `kan ikke lage pdf 2 ganger`() {
            brevService.lagBrev(behandlingPåklagetVedtak.id)
            brevService.lagBrevPdf(behandlingPåklagetVedtak.id)

            assertThatThrownBy { brevService.lagBrevPdf(behandlingPåklagetVedtak.id) }
                .hasMessage("Det finnes allerede en lagret pdf")
        }
    }
}
