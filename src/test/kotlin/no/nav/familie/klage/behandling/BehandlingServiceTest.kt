package no.nav.tilleggsstonader.klage.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.oppgave.OppgaveTaskService
import no.nav.tilleggsstonader.klage.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.tilleggsstonader.kontrakter.felles.klage.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.prosessering.domene.Task
import no.nav.tilleggsstonader.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class BehandlingServiceTest {

    val klageresultatRepository = mockk<KlageresultatRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val behandlinghistorikkService = mockk<BehandlingshistorikkService>()
    val taskService = mockk<TaskService>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()

    val behandlingService = BehandlingService(
        behandlingRepository,
        fagsakService,
        klageresultatRepository,
        behandlinghistorikkService,
        oppgaveTaskService,
        taskService,
        fagsystemVedtakService,
    )
    val behandlingSlot = slot<Behandling>()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        every { behandlinghistorikkService.opprettBehandlingshistorikk(any(), any()) } returns mockk()
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) } returns mockk()
        every { taskService.save(any()) } returns mockk<Task>()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class HenleggBehandling {

        private fun henleggOgForventOk(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
            assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingSlot.captured.vedtakDato).isNotNull
        }

        private fun henleggOgForventApiFeilmelding(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            val feil: ApiFeil = assertThrows {
                behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal kunne henlegge behandling`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, henlagtÅrsak = HenlagtÅrsak.FEILREGISTRERT)
            verify(exactly = 1) { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) }
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er oversendt kabal`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.VENTER)
            henleggOgForventApiFeilmelding(behandling, HenlagtÅrsak.FEILREGISTRERT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.FERDIGSTILT)
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        @Test
        internal fun `henlegg og forvent historikkinnslag`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, TRUKKET_TILBAKE)
            verify { behandlinghistorikkService.opprettBehandlingshistorikk(any(), StegType.BEHANDLING_FERDIGSTILT) }
            verify(exactly = 1) { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) }
        }
    }

    @Nested
    inner class PåklagetVedtak {

        @Test
        internal fun `skal ikke kunne oppdatere påklaget vedtak dersom behandlingen er låst`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.VENTER)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            assertThrows<ApiFeil> {
                behandlingService.oppdaterPåklagetVedtak(
                    behandlingId = behandling.id,
                    PåklagetVedtakDto(null, PåklagetVedtakstype.UTEN_VEDTAK),
                )
            }
        }

        @Test
        internal fun `skal ikke kunne oppdatere påklaget vedtak med ugyldig tilstand`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            val ugyldigManglerBehandlingId = PåklagetVedtakDto(null, PåklagetVedtakstype.VEDTAK)
            val ugyldigManglerVedtaksdatoInfotrygd = PåklagetVedtakDto(null, PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING)
            val ugyldigManglerVedtaksdatoUtestengelse = PåklagetVedtakDto(null, PåklagetVedtakstype.UTESTENGELSE)
            val ugyldigManglerVedtaksdatoInfotrygdOrdinærtVedtak = PåklagetVedtakDto(null, PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK)
            val ugyldigUtenVedtakMedBehandlingId = PåklagetVedtakDto("123", PåklagetVedtakstype.UTEN_VEDTAK)
            val ugyldigIkkeValgtMedBehandlingId = PåklagetVedtakDto("123", PåklagetVedtakstype.IKKE_VALGT)

            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigUtenVedtakMedBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigIkkeValgtMedBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoInfotrygd) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoUtestengelse) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoInfotrygdOrdinærtVedtak) }
        }

        @Test
        internal fun `skal kunne oppdatere påklaget vedtak med gyldige tilstander`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            mockHentFagsystemVedtak(behandling, "123")

            val medVedtak = PåklagetVedtakDto("123", PåklagetVedtakstype.VEDTAK)
            val utenVedtak = PåklagetVedtakDto(null, PåklagetVedtakstype.UTEN_VEDTAK)
            val ikkeValgt = PåklagetVedtakDto(null, PåklagetVedtakstype.IKKE_VALGT)
            val gjelderInfotrygd = PåklagetVedtakDto(null, PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING, manuellVedtaksdato = LocalDate.now())
            val utestengelse = PåklagetVedtakDto(null, PåklagetVedtakstype.UTESTENGELSE, manuellVedtaksdato = LocalDate.now())

            behandlingService.oppdaterPåklagetVedtak(behandling.id, ikkeValgt)
            verify(exactly = 1) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, utenVedtak)
            verify(exactly = 2) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, medVedtak)
            verify(exactly = 3) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, gjelderInfotrygd)
            verify(exactly = 4) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, utestengelse)
            verify(exactly = 5) { behandlingRepository.update(any()) }
        }
    }

    fun mockHentFagsystemVedtak(
        behandling: Behandling,
        eksternBehandlingId: String,
    ) {
        val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = eksternBehandlingId)
        every {
            fagsystemVedtakService.hentFagsystemVedtakForPåklagetBehandlingId(behandling.id, eksternBehandlingId)
        } returns fagsystemVedtak
    }
}
