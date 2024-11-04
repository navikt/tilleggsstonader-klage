package no.nav.tilleggsstonader.klage.personopplysninger

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fullmakt.FullmaktService
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstonaderSakClient
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.klage.personopplysninger.dto.FullmaktDto
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.klage.personopplysninger.dto.VergemålDto
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlSøker
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.gjeldende
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.visningsnavn
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val fullmaktService: FullmaktService,
    private val tilleggsstonaderSakClient: TilleggsstonaderSakClient,
) {
    @Cacheable("hentPersonopplysninger", cacheManager = "shortCache")
    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val søkersIdent = fagsak.hentAktivIdent()
        val egenAnsatt = tilleggsstonaderSakClient.erEgenAnsatt(søkersIdent)
        val harFullmektig = fullmaktService.hentFullmektige(søkersIdent).isNotEmpty()
        val pdlSøker = pdlClient.hentPerson(søkersIdent, fagsak.stønadstype)
        val andreParterNavn = hentNavnAndreParter(pdlSøker, fagsak.stønadstype)

        return PersonopplysningerDto(
            personIdent = søkersIdent,
            navn = pdlSøker.navn.gjeldende().visningsnavn(),
            adressebeskyttelse = pdlSøker.adressebeskyttelse.gjeldende()
                ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
            folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus.gjeldende()
                ?.let { Folkeregisterpersonstatus.fraPdl(it) },
            dødsdato = pdlSøker.dødsfall.gjeldende()?.dødsdato,
            fullmakt = mapFullmakt(pdlSøker, andreParterNavn),
            egenAnsatt = egenAnsatt,
            vergemål = mapVergemål(pdlSøker),
            harFullmektig = harFullmektig,
        )
    }

    /**
     * Returnerer map med ident og visningsnavn
     */
    private fun hentNavnAndreParter(pdlSøker: PdlSøker, stønadstype: Stønadstype): Map<String, String> {
        return pdlSøker.fullmakt.map { it.motpartsPersonident }.distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { hentNavn(it, stønadstype) }
            ?: emptyMap()
    }

    private fun hentNavn(it: List<String>, stønadstype: Stønadstype): Map<String, String> =
        pdlClient.hentNavnBolk(it, stønadstype).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()

    private fun mapFullmakt(pdlSøker: PdlSøker, andreParterNavn: Map<String, String>) = pdlSøker.fullmakt.map {
        FullmaktDto(
            gyldigFraOgMed = it.gyldigFraOgMed,
            gyldigTilOgMed = it.gyldigTilOgMed,
            motpartsPersonident = it.motpartsPersonident,
            navn = andreParterNavn[it.motpartsPersonident] ?: error("Finner ikke navn til ${it.motpartsPersonident}"),
            områder = it.omraader.map { område -> mapOmråde(område) },
        )
    }.sortedByDescending(FullmaktDto::gyldigFraOgMed)

    private fun mapVergemål(søker: PdlSøker) =
        søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
            VergemålDto(
                embete = it.embete,
                type = it.type,
                motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                navn = it.vergeEllerFullmektig.identifiserendeInformasjon?.navn?.visningsnavn(),
                omfang = it.vergeEllerFullmektig.omfang,
            )
        }

    private fun mapOmråde(område: String): String {
        return when (område) {
            "*" -> "ALLE"
            else -> område
        }
    }
}
