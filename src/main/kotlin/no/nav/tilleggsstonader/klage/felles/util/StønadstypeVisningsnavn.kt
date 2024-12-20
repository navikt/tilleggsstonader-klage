package no.nav.tilleggsstonader.klage.felles.util

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

object StønadstypeVisningsnavn {

    fun Stønadstype.visningsnavn() = when (this) {
        Stønadstype.BARNETILSYN -> "stønad til pass av barn"

        Stønadstype.LÆREMIDLER -> "støtte til læremidler"
    }
}
