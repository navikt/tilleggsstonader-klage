package no.nav.tilleggsstonader.klage.behandlingshistorikk.dto

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime

data class BehandlingshistorikkDto(
    val steg: StegType,
    val hendelse: Hendelse,
    val endretAvNavn: String,
    val endretTid: LocalDateTime,
    val metadata: Map<String, Any>? = null,
)

fun List<Behandlingshistorikk>.tilDto() =
    this
        .map {
            BehandlingshistorikkDto(
                steg = it.steg,
                hendelse = mapHendelse(it),
                metadata = it.metadata?.json?.let { jsonMapper.readValue<Map<String, Any>>(it) },
                endretAvNavn = it.opprettetAvNavn ?: it.opprettetAv,
                endretTid = it.endretTid,
            )
        }

private fun mapHendelse(behandlingshistorikk: Behandlingshistorikk): Hendelse =
    when (behandlingshistorikk.utfall) {
        StegUtfall.HENLAGT -> Hendelse.HENLAGT
        StegUtfall.SATT_PÅ_VENT -> Hendelse.SATT_PÅ_VENT
        StegUtfall.TATT_AV_VENT -> Hendelse.TATT_AV_VENT
        null -> mapFraSteg(behandlingshistorikk.steg)
    }

private fun mapFraSteg(steg: StegType): Hendelse =
    when (steg) {
        StegType.OPPRETTET -> Hendelse.OPPRETTET
        StegType.FORMKRAV -> Hendelse.FORMKRAV
        StegType.VURDERING -> Hendelse.VURDERING
        StegType.BREV -> Hendelse.BREV
        StegType.OVERFØRING_TIL_KABAL -> Hendelse.OVERFØRING_TIL_KABAL
        StegType.KABAL_VENTER_SVAR -> Hendelse.KABAL_VENTER_SVAR
        StegType.BEHANDLING_FERDIGSTILT -> Hendelse.BEHANDLING_FERDIGSTILT
    }

enum class Hendelse {
    OPPRETTET,
    FORMKRAV,
    VURDERING,
    BREV,
    OVERFØRING_TIL_KABAL,
    KABAL_VENTER_SVAR,
    BEHANDLING_FERDIGSTILT,

    // Fra utfall
    HENLAGT,
    SATT_PÅ_VENT,
    TATT_AV_VENT,
}
