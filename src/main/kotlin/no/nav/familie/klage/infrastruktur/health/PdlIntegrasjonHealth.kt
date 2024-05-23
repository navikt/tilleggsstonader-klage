package no.nav.tilleggsstonader.klage.infrastruktur.health

import no.nav.tilleggsstonader.http.health.AbstractHealthIndicator
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class PdlIntegrasjonHealth(client: PdlClient) :
    AbstractHealthIndicator(client, "pdl.personinfo")
