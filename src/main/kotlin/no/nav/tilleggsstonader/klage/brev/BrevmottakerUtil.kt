package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis

object BrevmottakerUtil {
    fun validerUnikeBrevmottakere(mottakere: Brevmottakere) {
        val personmottakerIdenter = mottakere.personer.map { it.personIdent }
        brukerfeilHvisIkke(personmottakerIdenter.distinct().size == personmottakerIdenter.size) {
            "En person kan bare legges til en gang som brevmottaker"
        }

        val organisasjonsmottakerIdenter = mottakere.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til en gang som brevmottaker"
        }
    }

    fun validerMinimumEnMottaker(mottakere: Brevmottakere) {
        feilHvis(mottakere.personer.isEmpty() && mottakere.organisasjoner.isEmpty()) {
            "Må ha minimum en mottaker"
        }
    }
}
