package no.nav.tilleggsstonader.klage.infrastruktur.health

import no.nav.tilleggsstonader.http.health.AbstractHealthIndicator
import no.nav.tilleggsstonader.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class FamilieIntegrasjonHealth(personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient) :
    AbstractHealthIndicator(personopplysningerIntegrasjonerClient, "familie.integrasjoner")
