package no.nav.tilleggsstonader.klage.behandlingshistorikk.dto

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import java.time.LocalDateTime

data class BehandlingshistorikkDto(
    val steg: StegType,
    val ufall: StegUtfall?,
    val endretAvNavn: String,
    val endretTid: LocalDateTime,
    val metadata: Map<String, Any>? = null,
)

fun List<Behandlingshistorikk>.tilDto() =
    this
        .map {
            BehandlingshistorikkDto(
                steg = it.steg,
                ufall = it.utfall,
                metadata = it.metadata?.json?.let { objectMapper.readValue<Map<String, Any>>(it) },
                endretAvNavn = it.opprettetAvNavn ?: it.opprettetAv,
                endretTid = it.endretTid,
            )
        }
