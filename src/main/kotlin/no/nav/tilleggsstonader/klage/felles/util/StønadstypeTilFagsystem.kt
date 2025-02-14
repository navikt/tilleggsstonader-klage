package no.nav.tilleggsstonader.klage.felles.util

import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

fun Stønadstype.tilFagsystem(): Fagsystem =
    when (this) {
        Stønadstype.BARNETILSYN,
        Stønadstype.LÆREMIDLER,
        -> Fagsystem.TILLEGGSSTONADER
    }
