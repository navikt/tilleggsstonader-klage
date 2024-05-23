package no.nav.tilleggsstonader.klage.felles.util

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

object StønadstypeVisningsnavn {

    fun Stønadstype.visningsnavn() = when (this) {
        Stønadstype.BARNETILSYN,
        Stønadstype.SKOLEPENGER,
        -> "stønad til ${this.name.lowercase()}"

        Stønadstype.OVERGANGSSTØNAD,
        Stønadstype.KONTANTSTØTTE,
        Stønadstype.BARNETRYGD,
        -> this.name.lowercase()
    }
}
