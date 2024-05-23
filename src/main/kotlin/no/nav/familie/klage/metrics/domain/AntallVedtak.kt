package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.kontrakter.felles.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.felles.klage.Stønadstype

data class AntallVedtak(
    val stonadstype: Stønadstype,
    val resultat: BehandlingResultat,
    val antall: Int,
)
