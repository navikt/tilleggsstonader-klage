package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.kabal.domain.Type
import no.nav.tilleggsstonader.klage.kabal.event.KabalBehandlingEventService
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID

/**
 * Håndterer meldinger fra Klageinstansen (KA) sitt saksbehandlingssystem Kabal.
 *
 * Behov for å lese fra en spesifikk offset? Se f.eks. commit e37f2f8e
 */
@Component
class KabalKafkaListener(
    val kabalBehandlingEventService: KabalBehandlingEventService,
) : ConsumerSeekAware {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    val støttedeFagsystemer = listOf(Fagsystem.TILLEGGSSTONADER.name)

    @KafkaListener(
        id = "tilleggsstonader-klage",
        topics = ["klage.behandling-events.v1"],
        autoStartup = "\${kafka.enabled:true}",
    )
    fun listen(behandlingEventJson: String) {
        secureLogger.info("Klage-kabal-event: $behandlingEventJson")
        val kabalBehandlingEvent = jsonMapper.readValue<KabalBehandlingEvent>(behandlingEventJson)

        if (støttedeFagsystemer.contains(kabalBehandlingEvent.kilde)) {
            kabalBehandlingEventService.handleEvent(kabalBehandlingEvent)
        }
        secureLogger.info("Serialisert behandlingEvent: $kabalBehandlingEvent")
    }
}

// se no.nav.tilleggsstonader.klage.kabal.OversendtKlageAnkeV3
data class KabalBehandlingEvent(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer,
) {
    fun mottattEllerAvsluttetTidspunkt(): LocalDateTime {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)

            BehandlingEventType.ANKEBEHANDLING_OPPRETTET ->
                detaljer.ankebehandlingOpprettet?.mottattKlageinstans ?: throw Feil(feilmelding)

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.avsluttet ?: throw Feil(
                    feilmelding,
                )

            BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET ->
                detaljer.ankeITrygderettenbehandlingOpprettet?.sendtTilTrygderetten ?: throw Feil(feilmelding)

            BehandlingEventType.BEHANDLING_FEILREGISTRERT ->
                detaljer.behandlingFeilregistrert?.feilregistrert
                    ?: throw Feil("Fant ikke tidspunkt for feilregistrering")

            BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET ->
                detaljer.behandlingEtterTrygderettenOpphevetAvsluttet?.avsluttet ?: throw Feil(feilmelding)

            BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET ->
                detaljer.omgjoeringskravbehandlingAvsluttet?.avsluttet
                    ?: throw Feil("Ikke implementert for OMGJOERINGSKRAVBEHANDLING_AVSLUTTET")
        }
    }

    fun utfall(): KlageinstansUtfall? {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.utfall ?: throw Feil(
                    feilmelding,
                )

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.utfall ?: throw Feil(
                    feilmelding,
                )

            else -> null
        }
    }

    fun journalpostReferanser(): List<String> =
        when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.journalpostReferanser
                    ?: listOf()

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.journalpostReferanser
                    ?: listOf()

            else -> listOf()
        }
}

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
    val behandlingFeilregistrert: BehandlingFeilregistrertDetaljer? = null,
    val ankeITrygderettenbehandlingOpprettet: AnkeITrygderettenbehandlingOpprettetDetaljer? = null,
    val behandlingEtterTrygderettenOpphevetAvsluttet: BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer? = null,
    val omgjoeringskravbehandlingAvsluttet: OmgjoeringskravbehandlingAvsluttetDetaljer? = null,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        klagebehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: ankebehandlingOpprettet?.oppgaveTekst(saksbehandlersEnhet)
            ?: ankebehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: behandlingEtterTrygderettenOpphevetAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: "Ukjent"
}

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type klagebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type ankebehandling opprettet mottatt. Mottatt klageinstans: $mottattKlageinstans. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet."
}

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type ankebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class BehandlingFeilregistrertDetaljer(
    val reason: String,
    val type: Type,
    val feilregistrert: LocalDateTime,
)

data class AnkeITrygderettenbehandlingOpprettetDetaljer(
    val sendtTilTrygderetten: LocalDateTime,
    val utfall: KlageinstansUtfall?,
)

data class BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type behandling etter trygderetten opphevet avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class OmgjoeringskravbehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String) =
        "Hendelse fra klage etter omgjøringskrav med utfall $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}
