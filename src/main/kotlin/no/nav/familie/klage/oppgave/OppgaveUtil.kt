package no.nav.tilleggsstonader.klage.oppgave

import no.nav.familie.util.VirkedagerProvider.nesteVirkedag //TODO: Vurder å fase ut familie-pakke
import java.time.LocalDate
import java.time.LocalDateTime

object OppgaveUtil {

    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val fristTilNesteVirkedag = nesteVirkedag(gjeldendeTid.toLocalDate())
        return if (gjeldendeTid.hour >= 12) {
            return nesteVirkedag(fristTilNesteVirkedag)
        } else {
            fristTilNesteVirkedag
        }
    }
}
