package no.nav.tilleggsstonader.klage.personopplysninger

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.tilleggsstonader.klage.PersonIdent
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattRequest
import no.nav.tilleggsstonader.klage.felles.dto.EgenAnsattResponse
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrasjonerConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class PersonopplysningerIntegrasjonerClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) :
    AbstractPingableRestClient(restOperations, "tilleggstonader.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return Tilgang(true)
        //TODO: Skal tilgangsjekker flyttes til integrasjoner, gjøres her, eller gjøres i sak?
//        return postForEntity(
//            integrasjonerConfig.tilgangRelasjonerUri,
//            PersonIdent(personIdent),
//            HttpHeaders().also {
//                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
//            },
//        )
    }

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(
            integrasjonerConfig.egenAnsattUri,
            EgenAnsattRequest(ident),
        ).data!!.erEgenAnsatt
    }

    companion object {

        const val HEADER_NAV_TEMA = "Nav-Tema"
        const val HEADER_NAV_TEMA_ENF = "ENF"
    }
}
