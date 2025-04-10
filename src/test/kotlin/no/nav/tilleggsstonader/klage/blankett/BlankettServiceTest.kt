package no.nav.tilleggsstonader.klage.blankett

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.brev.FamilieDokumentClient
import no.nav.tilleggsstonader.klage.brev.HtmlifyClient
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.formkrav.domain.FormkravFristUnntak
import no.nav.tilleggsstonader.klage.formkrav.dto.tilDto
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.personopplysninger.PersonopplysningerService
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.oppfyltForm
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurderingDto
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BlankettServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val formService = mockk<FormService>()
    private val vurderingService = mockk<VurderingService>()
    private val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    private val htmlifyClient = mockk<HtmlifyClient>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()

    private val service =
        BlankettService(
            fagsakService,
            behandlingService,
            personopplysningerService,
            formService,
            vurderingService,
            htmlifyClient,
            familieDokumentClient,
        )

    private val eksternFagsystemBehandlingId = "eksternFagsystemBehandlingId"

    private val blankettRequestSpot = slot<BlankettPdfRequest>()
    private val fagsak = fagsak(setOf(PersonIdent("ident")))
    private val behandling =
        behandling(
            fagsak = fagsak,
            påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer(eksternFagsystemBehandlingId)),
            klageMottatt = LocalDate.of(2022, 10, 26),
        )

    @BeforeEach
    internal fun setUp() {
        val behandlingId = behandling.id
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.navn } returns "navn"
        every { personopplysningerService.hentPersonopplysninger(behandlingId) } returns personopplysningerDto
        every { formService.hentFormDto(behandlingId) } returns
            oppfyltForm(behandlingId)
                .copy(
                    saksbehandlerBegrunnelse = "Ok",
                    brevtekst = "Brevtekst",
                    klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_SATT,
                ).tilDto(mockk())
        every { vurderingService.hentVurderingDto(behandlingId) } returns
            vurderingDto(
                vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                årsak = Årsak.FEIL_I_LOVANDVENDELSE,
                begrunnelseOmgjøring = "begrunnelse",
                hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                interntNotat = "interntNotat",
                innstillingKlageinstans = "innstillingKlageinstans",
            )
        every { htmlifyClient.genererBlankett(capture(blankettRequestSpot)) } returns "html"
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns byteArrayOf(1)
        every { fagsystemVedtakService.hentFagsystemVedtak(behandlingId) } returns
            listOf(
                fagsystemVedtak(eksternBehandlingId = eksternFagsystemBehandlingId),
            )
    }

    @Test
    internal fun `validerer json-request`() {
        service.lagBlankett(behandling.id)

        val blankettRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(blankettRequestSpot.captured)
        val expected =
            this::class.java.classLoader
                .getResource("blankett/request.json")!!
                .readText()
        assertThat(blankettRequest).isEqualTo(expected)
    }
}
