package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.kontrakter.felles.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.felles.klage.Stønadstype

data class BehandlingerPerStatus(
    val stonadstype: Stønadstype,
    val status: BehandlingStatus,
    val antall: Int,
)
