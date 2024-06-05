package no.nav.tilleggsstonader.klage.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class LenkeConfig(
    @Value("lenker.TILLEGGSSTONADER_SAK_FRONTEND_URL")
    val tilleggsstonaderSakLenke: String,
)
