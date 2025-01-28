package no.nav.tilleggsstonader.klage.felles.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TekstUtil {
    fun String.storForbokstav() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }

    object DatoFormat {
        val DATE_FORMAT_NORSK: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    fun LocalDate.norskFormat(): String = this.format(DatoFormat.DATE_FORMAT_NORSK)

    fun LocalDateTime.norskFormat(): String = this.format(DatoFormat.DATE_FORMAT_NORSK)
}
