package no.nav.tilleggsstonader.klage.infrastruktur.config

import com.fasterxml.jackson.databind.ObjectMapper

object ObjectMapperProvider {

    val objectMapper: ObjectMapper = no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
}
