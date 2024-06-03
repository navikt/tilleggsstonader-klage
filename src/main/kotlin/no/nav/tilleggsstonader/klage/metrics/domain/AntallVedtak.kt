package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat

data class AntallVedtak(
    val stonadstype: Stønadstype,
    val resultat: BehandlingResultat,
    val antall: Int,
)
