package no.nav.tilleggsstonader.klage.vurdering.dto

import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel

data class HjemmelDto(
    val hjemmel: Hjemmel,
    val visningstekst: String,
)

fun List<HjemmelDto>.tilHjemler() = map { it.hjemmel }
