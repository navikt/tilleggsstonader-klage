package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

data class ÅpneBehandlingerFraUke(
    val år: Int,
    val uke: Int,
    val stonadstype: Stønadstype,
    val antall: Int,
)
