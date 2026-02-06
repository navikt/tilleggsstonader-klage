package no.nav.tilleggsstonader.klage.infrastruktur.repository

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import tools.jackson.module.kotlin.readValue

data class JsonWrapper(
    val json: String,
)

fun JsonWrapper?.tilJson(): Map<String, Any>? = this?.json?.let { jsonMapper.readValue(it) }
