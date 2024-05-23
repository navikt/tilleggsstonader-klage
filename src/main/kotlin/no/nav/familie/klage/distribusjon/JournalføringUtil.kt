package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.AvsenderMottaker

object JournalføringUtil {

    fun mapAvsenderMottaker(brevmottakere: Brevmottakere): List<AvsenderMottaker> {
        return brevmottakere.let { mottakere ->
            mottakere.personer.map {
                AvsenderMottaker(
                    id = it.personIdent,
                    navn = it.navn,
                    idType = BrukerIdType.FNR,
                )
            } + mottakere.organisasjoner.map {
                AvsenderMottaker(
                    id = it.organisasjonsnummer,
                    navn = it.navnHosOrganisasjon,
                    idType = BrukerIdType.ORGNR,
                )
            }
        }
    }
}
