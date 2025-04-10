package no.nav.tilleggsstonader.klage.infrastruktur.repository

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper

data class JsonWrapper(
    val json: String,
)

fun JsonWrapper?.tilJson(): Map<String, Any>? = this?.json?.let { objectMapper.readValue(it) }
