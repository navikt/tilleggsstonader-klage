package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.klage.BehandlingStatus

data class BehandlingerPerStatus(
    val stonadstype: Stønadstype,
    val status: BehandlingStatus,
    val antall: Int,
)
