package no.nav.tilleggsstonader.klage.infrastruktur.health

import no.nav.tilleggsstonader.http.health.AbstractHealthIndicator
import no.nav.tilleggsstonader.klage.brev.BrevClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BrevHealth(client: BrevClient) :
    AbstractHealthIndicator(client, "familie.brev")
