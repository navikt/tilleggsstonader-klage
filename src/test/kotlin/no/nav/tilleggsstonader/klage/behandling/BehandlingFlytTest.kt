package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.dto.tilDto
import no.nav.tilleggsstonader.klage.infrastruktur.TestHendelseController
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.infrastruktur.config.RolleConfig
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurderingDto
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BehandlingFlytTest : IntegrationTest() {

    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var ferdigstillBehandlingService: FerdigstillBehandlingService

    @Autowired
    private lateinit var testHendelseController: TestHendelseController

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Nested
    inner class Historikk {

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - når man har sendt brev skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ts.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.KABAL_VENTER_SVAR)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.OVERFØRING_TIL_KABAL,
                StegType.BREV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.OPPRETTET,
            )
        }

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - skal kunne hoppe mellom steg`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ts.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))

                lagEllerOppdaterBrev(behandlingId)

                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId))

                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            testHendelseController.opprettDummyKabalEvent(behandlingId)

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.KABAL_VENTER_SVAR,
                StegType.OVERFØRING_TIL_KABAL,
                StegType.BREV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.OPPRETTET,
            )
        }

        @Test
        internal fun `OMGJØR_VEDTAK - når man har ferdigstilt klagebehandling skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ts.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(
                    vurderingDto(
                        behandlingId = behandlingId,
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                )
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.OPPRETTET,
            )
        }

        @Test
        internal fun `Ikke oppfylt formkrav skal ikke vurderes`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ts.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(ikkeOppfyltFormDto(behandlingId))
                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.BREV,
                StegType.FORMKRAV,
                StegType.OPPRETTET,
            )
        }

        private fun lagEllerOppdaterBrev(behandlingId: UUID) {
            brevService.lagBrev(behandlingId)
        }
    }

    private val påklagetVedtakDto = PåklagetVedtakDto(eksternFagsystemBehandlingId = "123", VEDTAK)

    private val opprettKlagebehandlingRequest =
        OpprettKlagebehandlingRequest(
            "ident",
            Stønadstype.BARNETILSYN,
            UUID.randomUUID().toString(),
            Fagsystem.TILLEGGSSTONADER,
            LocalDate.now(),
            "enhet",
        )

    private fun oppfyltFormDto(behandlingId: UUID, påklagetVedtakDto: PåklagetVedtakDto = DomainUtil.påklagetVedtakDto()) =
        DomainUtil.oppfyltForm(behandlingId).tilDto(påklagetVedtakDto)

    private fun ikkeOppfyltFormDto(behandlingId: UUID) =
        DomainUtil.oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(
            klagePart = FormVilkår.IKKE_OPPFYLT,
            saksbehandlerBegrunnelse = "Ok",
            brevtekst = "brevtekst",
        )
}
