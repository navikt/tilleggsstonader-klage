package no.nav.tilleggsstonader.klage.personopplysninger.pdl

import no.nav.tilleggsstonader.klage.infrastruktur.config.PdlConfig
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class PdlClient(
    @Value("\${PDL_URL}") private val pdlUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    val pdlUri = UriComponentsBuilder.fromUri(pdlUrl).pathSegment(PdlConfig.PATH_GRAPHQL).toUriString()

    @Cacheable("hentPerson", cacheManager = "shortCache")
    fun hentPerson(
        personIdent: String,
        stønadstype: Stønadstype,
    ): PdlSøker {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.søkerQuery,
            )
        val pdlResponse: PdlResponse<PdlSøkerData> =
            postForEntity(
                pdlUri,
                pdlPersonRequest,
                httpHeaders(stønadstype.tilTema()),
            )
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    @Cacheable("hentNavnBolk", cacheManager = "shortCache")
    fun hentNavnBolk(
        personIdenter: List<String>,
        stønadstype: Stønadstype,
    ): Map<String, PdlNavn> {
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.bolkNavnQuery,
            )
        val pdlResponse: PdlBolkResponse<PdlNavn> =
            postForEntity(
                pdlUri,
                pdlPersonRequest,
                httpHeaders(stønadstype.tilTema()),
            )
        return feilsjekkOgReturnerData(pdlResponse)
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @param historikk default false, tar med historikk hvis det er ønskelig
     * @return liste med folkeregisteridenter
     */
    @Cacheable("personidenter", cacheManager = "shortCache")
    fun hentPersonidenter(
        ident: String,
        tema: Tema,
        historikk: Boolean = false,
    ): PdlIdenter {
        val pdlIdentRequest =
            PdlIdentRequest(
                variables = PdlIdentRequestVariables(ident, "FOLKEREGISTERIDENT", historikk),
                query = PdlConfig.hentIdentQuery,
            )
        val pdlResponse: PdlResponse<PdlHentIdenter> =
            postForEntity(
                pdlUri,
                pdlIdentRequest,
                httpHeaders(tema),
            )
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    fun hentPersonidenter(
        ident: String,
        stønadstype: Stønadstype,
        historikk: Boolean = false,
    ): PdlIdenter = hentPersonidenter(ident, stønadstype.tilTema(), historikk)

    private fun httpHeaders(tema: Tema): HttpHeaders =
        HttpHeaders().apply {
            add("Tema", tema.name)
            add("behandlingsnummer", "B289") // Behandlingsnummer: B289
        }
}
