package no.nav.tilleggsstonader.klage.brev.dto

import no.nav.tilleggsstonader.klage.brev.domain.Avsnitt

data class AvsnittDto(
    val deloverskrift: String,
    val innhold: String,
    val skalSkjulesIBrevbygger: Boolean? = false,
)

fun Avsnitt.tilDto(): AvsnittDto =
    AvsnittDto(
        deloverskrift = deloverskrift,
        innhold = innhold,
        skalSkjulesIBrevbygger = skalSkjulesIBrevbygger,
    )
