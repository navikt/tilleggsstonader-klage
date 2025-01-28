package no.nav.tilleggsstonader.klage.brev.dto

import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerPerson
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere

data class BrevmottakereDto(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>,
)

fun Brevmottakere.tilDto() =
    BrevmottakereDto(
        personer = this.personer,
        organisasjoner = this.organisasjoner,
    )

fun BrevmottakereDto.tilDomene() =
    Brevmottakere(
        personer = this.personer,
        organisasjoner = this.organisasjoner,
    )
