package no.nav.tilleggsstonader.klage.felles.util

import java.time.LocalDate

fun LocalDate.isEqualOrBefore(other: LocalDate) = this == other || this.isBefore(other)
