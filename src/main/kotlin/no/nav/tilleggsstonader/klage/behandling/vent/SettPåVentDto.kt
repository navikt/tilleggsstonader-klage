package no.nav.tilleggsstonader.klage.behandling.vent

import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
import java.time.LocalDate
import java.time.LocalDateTime

data class SettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val beholdOppgave: Boolean = false,
) {
    init {
        brukerfeilHvis(årsaker.any { it == ÅrsakSettPåVent.ANNET } && kommentar.isNullOrBlank()) {
            "Kommentar er påkrevd ved valg av årsak 'Annet'"
        }
        brukerfeilHvis((kommentar?.length ?: 0) > 1000) {
            "Kommentar kan maks være 1000 tegn"
        }
    }
}

data class OppdaterSettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val oppgaveVersjon: Int,
    val beholdOppgave: Boolean = false,
)

data class StatusPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val kommentar: String?,
    val datoSattPåVent: LocalDate,
    val opprettetAv: String,
    val endretAv: String?,
    val endretTid: LocalDateTime?,
    val frist: LocalDate?,
    val oppgaveVersjon: Int,
)

data class TaAvVentDto(
    val skalTilordnesRessurs: Boolean,
    val kommentar: String?,
)

enum class ÅrsakSettPåVent {
    DOKUMENTASJON_FRA_BRUKER,
    VURDERING_AV_NEDSATT_ARBEIDSEVNE,
    REGISTRERING_AV_TILTAK,
    REGISTRERING_AV_UTDANNING,
    ANNET,
}
