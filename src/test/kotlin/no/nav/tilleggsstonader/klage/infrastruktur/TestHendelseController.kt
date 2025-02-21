package no.nav.tilleggsstonader.klage.infrastruktur

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.kabal.AnkebehandlingAvsluttetDetaljer
import no.nav.tilleggsstonader.klage.kabal.AnkebehandlingOpprettetDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingDetaljer
import no.nav.tilleggsstonader.klage.kabal.BehandlingEvent
import no.nav.tilleggsstonader.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.tilleggsstonader.klage.kabal.event.BehandlingEventService
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test/kabal"])
@Validated
@Unprotected
class TestHendelseController(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingEventService: BehandlingEventService,
) {
    @GetMapping("{behandlingId}")
    fun hentBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    @PostMapping
    fun opprettKabalEvent(
        @RequestBody behandlingEvent: BehandlingEvent,
    ) {
        behandlingEventService.handleEvent(behandlingEvent)
    }

    @PostMapping("{behandlingId}/dummy")
    fun opprettDummyKabalEvent(
        @PathVariable behandlingId: BehandlingId,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(
            BehandlingEvent(
                eventId = UUID.randomUUID(),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                kilde = Fagsystem.TILLEGGSSTONADER.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                detaljer =
                    BehandlingDetaljer(
                        KlagebehandlingAvsluttetDetaljer(
                            avsluttet = LocalDateTime.now(),
                            utfall = KlageinstansUtfall.AVVIST,
                            journalpostReferanser = listOf("journalpost1"),
                        ),
                    ),
            ),
        )
    }

    @PostMapping("{behandlingId}/startanke")
    fun opprettDummyKabalAnkeEvent(
        @PathVariable behandlingId: BehandlingId,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(
            BehandlingEvent(
                eventId = UUID.randomUUID(),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                kilde = Fagsystem.TILLEGGSSTONADER.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
                detaljer =
                    BehandlingDetaljer(
                        ankebehandlingOpprettet =
                            AnkebehandlingOpprettetDetaljer(
                                mottattKlageinstans = LocalDateTime.now(),
                            ),
                    ),
            ),
        )
    }

    @PostMapping("{behandlingId}/avsluttanke")
    fun opprettDummyKabalAvsluttAnkeEvent(
        @PathVariable behandlingId: BehandlingId,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(
            BehandlingEvent(
                eventId = UUID.randomUUID(),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                kilde = Fagsystem.TILLEGGSSTONADER.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                detaljer =
                    BehandlingDetaljer(
                        ankebehandlingAvsluttet =
                            AnkebehandlingAvsluttetDetaljer(
                                avsluttet = LocalDateTime.now(),
                                utfall = KlageinstansUtfall.DELVIS_MEDHOLD,
                                journalpostReferanser = listOf("1", "2", "3"),
                            ),
                    ),
            ),
        )
    }
}
