package no.nav.tilleggsstonader.klage.formkrav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.formkrav.domain.Form
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.dto.tilDto
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.oppfyltForm
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

internal class FormServiceTest {
    private val formRepository = mockk<FormRepository>()
    private val stegService = mockk<StegService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vurderingService = mockk<VurderingService>()
    private val taskService = mockk<TaskService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val service =
        FormService(
            formRepository,
            stegService,
            behandlingService,
            behandlingshistorikkService,
            vurderingService,
            taskService,
        )

    private val behandlingId = BehandlingId.random()

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)
        val behandlingshistorikk = mockk<Behandlingshistorikk>()
        every { behandlingshistorikk.steg } returns StegType.FORMKRAV
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler1"
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { behandlingService.oppdaterPåklagetVedtak(any(), any()) }
        justRun { vurderingService.slettVurderingForBehandling(any()) }
        every { behandlingService.hentBehandling(any()) } returns behandling(id = behandlingId)
        every { formRepository.findByIdOrNull(any()) } returns Form(behandlingId)
        every { formRepository.update(any()) } answers { firstArg() }
        every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(behandlingshistorikk)
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OppdaterFormkrav {
        @Test
        internal fun `ikke alle vilkår besvart skal gå til steget formKrav`() {
            service.oppdaterFormkrav(ikkeFerdigutfylt())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ikke valgt påklaget vedtak skal gå til steget formKrav`() {
            service.oppdaterFormkrav(
                oppfyltFormDto().copy(
                    påklagetVedtak =
                        DomainUtil
                            .påklagetVedtakDto()
                            .copy(påklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT),
                ),
            )

            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og oppfylte vilkår skal gå videre til vurdering`() {
            service.oppdaterFormkrav(oppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og oppfylte vilkår skal ikke behøve begrunnelse`() {
            service.oppdaterFormkrav(oppfyltFormDto().copy(saksbehandlerBegrunnelse = ""))

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og underkjente vilkår uten begrunnelse skal gå til steget formkrav`() {
            service.oppdaterFormkrav(ikkeOppfyltFormDto().copy(saksbehandlerBegrunnelse = ""))

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og underkjente vilkår med begrunnelse skal gå videre til brev`() {
            service.oppdaterFormkrav(ikkeOppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.BREV) }
            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ingen behandlingshistorikk av StegType FORMKRAV, skal opprette task for statistikk`() {
            val behandlingshistorikk =
                Behandlingshistorikk(
                    id = UUID.randomUUID(),
                    behandlingId = BehandlingId.random(),
                    steg = StegType.OPPRETTET,
                    utfall = null,
                    gitVersjon = null,
                )
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(behandlingshistorikk)
            every { taskService.save(any()) } returns mockk<Task>()
            service.oppdaterFormkrav(oppfyltFormDto())
            verify { taskService.save(any()) }
        }

        @Test
        internal fun `finnes behandlingshistorikk av StegType FORMKRAV, skal ikke opprette task for statistikk`() {
            val behandlingshistorikk =
                Behandlingshistorikk(
                    id = UUID.randomUUID(),
                    behandlingId = BehandlingId.random(),
                    steg = StegType.FORMKRAV,
                    utfall = null,
                    gitVersjon = null,
                )
            every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(behandlingshistorikk)
            every { taskService.save(any()) } returns mockk<Task>()
            service.oppdaterFormkrav(oppfyltFormDto())
            verify(exactly = 0) { taskService.save(any()) }
        }
    }

    private fun oppfyltFormDto() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto())

    private fun ikkeOppfyltFormDto() =
        oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(
            klagePart = FormVilkår.IKKE_OPPFYLT,
            saksbehandlerBegrunnelse = "Ok",
            brevtekst = "brevtekst",
        )

    private fun ikkeFerdigutfylt() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_SATT)
}
