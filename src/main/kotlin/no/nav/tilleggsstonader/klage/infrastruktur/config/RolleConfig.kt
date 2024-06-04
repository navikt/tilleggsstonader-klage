package no.nav.tilleggsstonader.klage.infrastruktur.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rolle")
class RolleConfig(
    val ts: FagsystemRolleConfig,
)

data class FagsystemRolleConfig(
    val saksbehandler: String,
    val beslutter: String,
    val veileder: String,
)
