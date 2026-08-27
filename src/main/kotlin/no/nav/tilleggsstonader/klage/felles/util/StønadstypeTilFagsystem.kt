package no.nav.tilleggsstonader.klage.felles.util

import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

fun Stønadstype.tilFagsystem(): Fagsystem =
    when (this) {
        Stønadstype.BARNETILSYN,
        Stønadstype.LÆREMIDLER,
        Stønadstype.BOUTGIFTER,
        Stønadstype.DAGLIG_REISE_TSO,
        Stønadstype.DAGLIG_REISE_TSR,
        Stønadstype.REISE_TIL_SAMLING_TSO,
        Stønadstype.REISE_TIL_SAMLING_TSR,
        Stønadstype.FLYTTING_TSR,
        Stønadstype.FLYTTING_TSO,
        Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
        Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
        -> Fagsystem.TILLEGGSSTONADER
    }
