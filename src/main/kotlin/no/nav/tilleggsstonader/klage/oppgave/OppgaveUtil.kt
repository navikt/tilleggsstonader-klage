package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.libs.utils.VirkedagerProvider.nesteVirkedag
import java.time.LocalDate
import java.time.LocalDateTime

object OppgaveUtil {
    /**
     * Oppretter frist for oppgave basert på kravMottatt eller behandlingOpprettet.
     * Hvis kravMottatt er null, brukes behandlingOpprettet.
     */
    fun fristBehandleSakOppgave(
        klageMottatt: LocalDate,
        behandlingOpprettet: LocalDateTime,
    ) = lagFristForOppgave(klageMottatt.atTime(behandlingOpprettet.toLocalTime()))

    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val fristTilNesteVirkedag = nesteVirkedag(gjeldendeTid.toLocalDate())
        return if (gjeldendeTid.hour >= 12) {
            return nesteVirkedag(fristTilNesteVirkedag)
        } else {
            fristTilNesteVirkedag
        }
    }
}
