package no.nav.tilleggsstonader.klage.behandling

import io.mockk.every
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.behandling.dto.tilPåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.infrastruktur.config.TSSakSakClientMock
import no.nav.tilleggsstonader.klage.integrasjoner.TSSakClient
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime.now

internal class BehandlingServiceIntegrasjonTest : IntegrationTest() {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var tsSakClientMock: TSSakClient

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext()
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @AfterEach
    internal fun tearDown() {
        TSSakSakClientMock.resetMock(tsSakClientMock)
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal oppdatere behandlingsresultat og vedtakstidspunkt`() {
        val persistertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(persistertBehandling.vedtakDato).isNull()
        assertThat(persistertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)

        behandlingService.oppdaterBehandlingMedResultat(behandling.id, BehandlingResultat.IKKE_MEDHOLD, null)
        val oppdatertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(oppdatertBehandling.vedtakDato).isEqualToIgnoringMinutes(now())
        assertThat(oppdatertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
    }

    @Nested
    inner class OppdaterPåklagetVedtak {

        @Test
        internal fun `skal oppdatere påklaget vedtak`() {
            val påklagetBehandlingId = "påklagetBehandlingId"
            val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = påklagetBehandlingId)
            every { tsSakClientMock.hentVedtak(fagsak.eksternId) } returns listOf(fagsystemVedtak)

            val påklagetVedtak = PåklagetVedtakDto(
                eksternFagsystemBehandlingId = påklagetBehandlingId,
                påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                manuellVedtaksdato = LocalDate.now() ,
            )
            behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
            val oppdatertPåklagetVedtak = oppdatertBehandling.påklagetVedtak

            assertThat(oppdatertPåklagetVedtak.påklagetVedtakstype).isEqualTo(påklagetVedtak.påklagetVedtakstype)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer).isEqualTo(fagsystemVedtak.tilPåklagetVedtakDetaljer())
        }


        @Test
        internal fun `skal feile hvis påklaget vedtak ikke finnes`() {
            every { tsSakClientMock.hentVedtak(fagsak.eksternId) } returns emptyList()
            val påklagetVedtak =
                PåklagetVedtakDto(eksternFagsystemBehandlingId = "finner ikke", påklagetVedtakstype = PåklagetVedtakstype.VEDTAK, manuellVedtaksdato = LocalDate.now())
            assertThatThrownBy {
                behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }
}
