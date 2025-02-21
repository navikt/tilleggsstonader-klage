package no.nav.tilleggsstonader.klage.felles.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DatoFormat {
    val DATE_FORMAT_ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM")
    val YEAR_MONTH_FORMAT_NORSK = DateTimeFormatter.ofPattern("MM.yyyy")
    val DATE_FORMAT_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val GOSYS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm")
}

fun LocalDate.isEqualOrBefore(other: LocalDate) = this == other || this.isBefore(other)

fun LocalDateTime.medGosysTid(): String = this.format(DatoFormat.GOSYS_DATE_TIME)
