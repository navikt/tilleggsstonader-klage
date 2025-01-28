package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlIdent
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlIdenter
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.pdlNavn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-pdl")
class PdlClientMock {
    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every {
            pdlClient.hentNavnBolk(
                any(),
                any(),
            )
        } answers { firstArg<List<String>>().associateWith { pdlNavn(listOf(lagNavn())) } }

        every { pdlClient.hentPerson(any(), any()) } returns opprettPdlSøker()

        every { pdlClient.hentPersonidenter(any(), any<Stønadstype>(), eq(true)) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        return pdlClient
    }

    companion object {
        private const val ANNEN_FORELDER_FNR = "17097926735"

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse =
                    listOf(
                        Adressebeskyttelse(
                            gradering = AdressebeskyttelseGradering.UGRADERT,
                            metadata = metadataGjeldende,
                        ),
                    ),
                dødsfall = listOf(),
                navn = listOf(lagNavn()),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(),
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> =
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            identifiserendeInformasjon = IdentifiserendeInformasjon(navn = null),
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "stadfestetFremtidsfullmakt",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            identifiserendeInformasjon = null,
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
            )
    }
}
