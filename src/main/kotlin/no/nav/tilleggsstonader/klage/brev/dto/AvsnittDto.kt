package no.nav.tilleggsstonader.klage.brev.dto

import no.nav.tilleggsstonader.klage.brev.domain.Avsnitt

data class AvsnittDto(
    val deloverskrift: String,
    val innhold: String,
)

fun Avsnitt.tilDto(): AvsnittDto =
    AvsnittDto(
        deloverskrift = deloverskrift,
        innhold = innhold,
    )
