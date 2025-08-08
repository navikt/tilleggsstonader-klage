package no.nav.tilleggsstonader.klage.oppgave

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.KLAGE_GJELDER_TILBAKEKREVING_METADATA_KEY
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_KLAR
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Properties

internal class OpprettBehandleSakOppgaveTaskTest {
    val oppgaveService = mockk<OppgaveService>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()

    val opprettBehandleSakOppgaveTask =
        OpprettBehandleSakOppgaveTask(
            fagsakService = fagsakService,
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
        )

    val fagsak = DomainUtil.fagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak)
    val oppgaveId = 1L

    lateinit var oppgaveSlot: CapturingSlot<OpprettOppgaveRequest>

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext()
        oppgaveSlot = slot()
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { oppgaveService.finnMappe("4462", OppgaveMappe.KLAR) } returns
            MappeDto(MAPPE_ID_KLAR, OppgaveMappe.KLAR.navn.first(), "4462", fagsak.stønadstype.tilTema().name)
        every { oppgaveService.opprettOppgave(any(), capture(oppgaveSlot)) } returns oppgaveId
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class OpprettBehandleSakOppgave {
        @Test
        internal fun `skal opprette behandleSak oppgave med riktige verdier for ny klagebehandling`() {
            val behandleSakOppgaveTask =
                Task(
                    type = OpprettBehandleSakOppgaveTask.TYPE,
                    payload = behandling.id.toString(),
                    properties =
                        Properties().apply {
                            this[SAKSBEHANDLER_METADATA_KEY] = ""
                        },
                )

            opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask)

            assertThat(oppgaveSlot.captured.behandlingstema).isEqualTo("ab0300")
            assertThat(oppgaveSlot.captured.behandlingstype).isEqualTo("ae0058")
            assertThat(oppgaveSlot.captured.behandlesAvApplikasjon).isEqualTo("tilleggsstonader-klage")
            assertThat(oppgaveSlot.captured.oppgavetype).isEqualTo(Oppgavetype.BehandleSak)
            assertThat(oppgaveSlot.captured.enhetsnummer).isEqualTo("4462")
            assertThat(oppgaveSlot.captured.fristFerdigstillelse).isAfter(LocalDate.now())
            assertThat(oppgaveSlot.captured.saksreferanse).isEqualTo(fagsak.eksternId)
            assertThat(oppgaveSlot.captured.tema).isEqualTo(Tema.TSO)
            assertThat(oppgaveSlot.captured.tilordnetRessurs).isNotNull
        }

        @Test
        internal fun `skal opprette behandleSakOppgave med behandlingstema klage tilbakekreving`() {
            val behandleSakOppgaveTask =
                Task(
                    type = OpprettBehandleSakOppgaveTask.TYPE,
                    payload = behandling.id.toString(),
                    properties =
                        Properties().apply {
                            this[KLAGE_GJELDER_TILBAKEKREVING_METADATA_KEY] = true.toString()
                        },
                )

            opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask)

            assertThat(oppgaveSlot.captured.behandlingstema).isEqualTo("ab0300")
        }
    }
}
