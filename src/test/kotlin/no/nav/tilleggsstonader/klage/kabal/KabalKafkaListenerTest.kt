package no.nav.tilleggsstonader.klage.kabal

import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.kabal.event.KabalBehandlingEventService
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KabalKafkaListenerTest {
    private lateinit var listener: KabalKafkaListener

    private val kabalBehandlingEventService = mockk<KabalBehandlingEventService>(relaxed = true)

    @BeforeEach
    internal fun setUp() {
        listener = KabalKafkaListener(kabalBehandlingEventService)
    }

    @Test
    internal fun `skal kalle på oppgaveService for hendelse med Tilleggsstønader som kilde`() {
        listener.listen(lagBehandlingEvent("TILLEGGSSTONADER"))

        verify(exactly = 1) { kabalBehandlingEventService.handleEvent(any()) }
    }

    @Test
    internal fun `skal ikke kalle på oppgaveService for hendelse med annen kilde enn EF`() {
        listener.listen(lagBehandlingEvent("AO01"))

        verify(exactly = 0) { kabalBehandlingEventService.handleEvent(any()) }
    }

    private fun lagBehandlingEvent(kilde: String): String {
        val kabalBehandlingEvent =
            KabalBehandlingEvent(
                UUID.randomUUID(),
                "kildeReferanse",
                kilde,
                "kabalReferanse",
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                BehandlingDetaljer(
                    KlagebehandlingAvsluttetDetaljer(
                        LocalDateTime.now().minusDays(1),
                        KlageinstansUtfall.MEDHOLD,
                        listOf("journalpostReferanse1", "journalpostReferanse2"),
                    ),
                ),
            )
        return jsonMapper.writeValueAsString(kabalBehandlingEvent)
    }
}
