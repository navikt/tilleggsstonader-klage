package no.nav.tilleggsstonader.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.kabal.AnkeITrygderettenbehandlingOpprettetDetaljer
import no.nav.tilleggsstonader.klage.kabal.AnkebehandlingOpprettetDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingEvent
import no.nav.tilleggsstonader.klage.kabal.BehandlingFeilregistrertDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingFeilregistrertTask
import no.nav.tilleggsstonader.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.kabal.OmgjoeringskravbehandlingAvsluttetDetaljer
import no.nav.tilleggsstonader.klage.kabal.Type
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingEventServiceTest {

    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val stegService = mockk<StegService>(relaxed = true)
    private val klageresultatRepository = mockk<KlageresultatRepository>(relaxed = true)
    private val integrasjonerClient = mockk<TilleggsstønaderIntegrasjonerClient>(relaxed = true)

    val behandlingEventService = BehandlingEventService(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        stegService = stegService,
        taskService = taskService,
        klageresultatRepository = klageresultatRepository,
        integrasjonerClient = integrasjonerClient,
    )

    val behandlingMedStatusVenter = DomainUtil.behandling(status = BehandlingStatus.VENTER)

    @BeforeEach
    fun setUp() {
        every { taskService.save(any()) } answers { firstArg() }
        every { behandlingRepository.findByEksternBehandlingId(any()) } returns behandlingMedStatusVenter
        every { klageresultatRepository.insert(any()) } answers { firstArg() }
        every { klageresultatRepository.existsById(any()) } returns false
        every { integrasjonerClient.hentSaksbehandlerInfo(any()) } returns saksbehandler
    }

    @Test
    fun `Skal lage oppgave og ferdigstille behandling for klage som ikke er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) {
            stegService.oppdaterSteg(
                behandlingMedStatusVenter.id,
                any(),
                StegType.BEHANDLING_FERDIGSTILT,
            )
        }
    }

    @Test
    fun `Skal ikke ferdigstille behandling, og ikke lage oppgave, når event er av type anke`() {
        val behandlingEvent = lagBehandlingEvent(
            behandlingEventType = BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
            behandlingDetaljer = BehandlingDetaljer(
                ankebehandlingOpprettet = AnkebehandlingOpprettetDetaljer(LocalDateTime.now()),
            ),
        )

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(any(), any(), any()) }
    }

    @Test
    fun `Skal ikke behandle klage som er er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()
        val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingRepository.findByEksternBehandlingId(any()) } returns behandling
        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(behandling.id, any(), StegType.BEHANDLING_FERDIGSTILT) }
    }

    @Test
    internal fun `Skal ikke behandle event hvis det allerede er behandlet`() {
        every { klageresultatRepository.existsById(any()) } returns true

        behandlingEventService.handleEvent(lagBehandlingEvent())

        verify(exactly = 0) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 0) { klageresultatRepository.insert(any()) }
    }

    @Test
    internal fun `Skal lagre event hvis det ikke allerede er behandlet`() {
        behandlingEventService.handleEvent(lagBehandlingEvent())

        verify(exactly = 1) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 1) { klageresultatRepository.insert(any()) }
    }

    @Test
    internal fun `Skal kunne lagre resultat for behandlingsevent ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET`() {
        val klageinstansResultatSlot = slot<KlageinstansResultat>()

        behandlingEventService.handleEvent(
            lagBehandlingEvent(
                BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
                BehandlingDetaljer(ankeITrygderettenbehandlingOpprettet = AnkeITrygderettenbehandlingOpprettetDetaljer(LocalDateTime.of(2023, 6, 21, 1, 1), null)),
            ),
        )

        verify(exactly = 1) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 1) { klageresultatRepository.insert(capture(klageinstansResultatSlot)) }

        assertThat(klageinstansResultatSlot.captured.type).isEqualTo(BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET)
    }

    @Test
    internal fun `Skal kunne lagre resultat for behandlingsevent omgjøringskrav`() {
        val klageinstansResultatSlot = slot<KlageinstansResultat>()

        val omgjoeringskravbehandlingAvsluttet = OmgjoeringskravbehandlingAvsluttetDetaljer(
            LocalDateTime.of(2023, 6, 21, 1, 1),
            KlageinstansUtfall.HEVET,
            emptyList(),
        )
        behandlingEventService.handleEvent(
            lagBehandlingEvent(
                BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET,
                BehandlingDetaljer(omgjoeringskravbehandlingAvsluttet = omgjoeringskravbehandlingAvsluttet),
            ),
        )

        verify(exactly = 1) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 1) { klageresultatRepository.insert(capture(klageinstansResultatSlot)) }

        assertThat(klageinstansResultatSlot.captured.type).isEqualTo(BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET)
    }

    @Test
    internal fun `Skal kunne lagre resultat for behandlingsevent BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET`() {
        val klageinstansResultatSlot = slot<KlageinstansResultat>()

        val detaljer = BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer(
            avsluttet = LocalDateTime.of(2023, 6, 21, 1, 1),
            utfall = KlageinstansUtfall.HEVET,
            journalpostReferanser = emptyList(),
        )
        behandlingEventService.handleEvent(
            lagBehandlingEvent(
                BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
                BehandlingDetaljer(behandlingEtterTrygderettenOpphevetAvsluttet = detaljer),
            ),
        )

        verify(exactly = 1) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 1) { klageresultatRepository.insert(capture(klageinstansResultatSlot)) }

        assertThat(klageinstansResultatSlot.captured.type)
            .isEqualTo(BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET)
    }

    @Test
    internal fun `Skal ikke ferdigstille behandling for behandlingsevent ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET`() {
        val klageinstansResultatSlot = slot<KlageinstansResultat>()

        behandlingEventService.handleEvent(
            lagBehandlingEvent(
                BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
                BehandlingDetaljer(ankeITrygderettenbehandlingOpprettet = AnkeITrygderettenbehandlingOpprettetDetaljer(LocalDateTime.of(2023, 6, 21, 1, 1), null)),
            ),
        )

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(any(), any(), any()) }
    }

    @Test
    internal fun `Skal opprette task for behandlingsevent BEHANDLING_FEILREGISTRERT`() {
        val taskSlot = slot<Task>()

        val behandlingFeilregistrertDetaljer = BehandlingFeilregistrertDetaljer("Fordi", Type.KLAGE, LocalDateTime.of(2023, 6, 21, 1, 1))

        every { taskService.save(capture(taskSlot)) } returns mockk()

        behandlingEventService.handleEvent(lagBehandlingEvent(BehandlingEventType.BEHANDLING_FEILREGISTRERT, BehandlingDetaljer(behandlingFeilregistrert = behandlingFeilregistrertDetaljer)))

        assertThat(taskSlot.captured.type).isEqualTo(BehandlingFeilregistrertTask.TYPE)
        assertThat(taskSlot.captured.payload).isEqualTo(behandlingMedStatusVenter.id.toString())
    }

    @Test
    internal fun `Skal lage OpprettOppgave-task for behandlingsevent BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET`() {
        val taskSlot = slot<Task>()

        every { taskService.save(capture(taskSlot)) } returns mockk()

        val detaljer = BehandlingDetaljer(
            behandlingEtterTrygderettenOpphevetAvsluttet = BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer(
                avsluttet = LocalDateTime.now(),
                utfall = KlageinstansUtfall.HEVET,
                journalpostReferanser = emptyList(),
            ),
        )
        val event = lagBehandlingEvent(
            behandlingEventType = BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
            behandlingDetaljer = detaljer,
        )
        behandlingEventService.handleEvent(event)

        assertThat(taskSlot.captured.type).isEqualTo(OpprettKabalEventOppgaveTask.TYPE)
        assertThat(taskSlot.captured.payload).contains("Hendelse fra klage av type behandling etter trygderetten opphevet avsluttet med utfall: HEVET mottatt.")
    }

    private fun lagBehandlingEvent(
        behandlingEventType: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        behandlingDetaljer: BehandlingDetaljer? = null,
    ): BehandlingEvent {
        return BehandlingEvent(
            eventId = UUID.randomUUID(),
            kildeReferanse = UUID.randomUUID().toString(),
            kilde = "TS",
            kabalReferanse = "kabalReferanse",
            type = behandlingEventType,
            detaljer = behandlingDetaljer ?: BehandlingDetaljer(
                KlagebehandlingAvsluttetDetaljer(
                    LocalDateTime.now().minusDays(1),
                    KlageinstansUtfall.MEDHOLD,
                    listOf("journalpostReferanse1", "journalpostReferanse2"),
                ),
            ),
        )
    }

    private val saksbehandler = Saksbehandler(
        UUID.randomUUID(),
        "A123456",
        "Alfa",
        "Omega",
        "4402",
    )
}
