package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Fullmakt
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.KjønnType
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlIdent
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlIdenter
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.lagKjønn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.pdlNavn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-pdl")
class PdlClientMock {

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.hentNavnBolk(any(), any()) } answers { firstArg<List<String>>().associateWith { pdlNavn(listOf(lagNavn())) } }

        every { pdlClient.hentPerson(any(), any()) } returns opprettPdlSøker()

        every { pdlClient.hentPersonidenter(any(), Stønadstype.BARNETILSYN, eq(true)) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        return pdlClient
    }

    companion object {

        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        private const val annenForelderFnr = "17097926735"

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse = listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende,
                    ),
                ),
                dødsfall = listOf(),
                fullmakt = fullmakter(),
                kjønn = lagKjønn(KjønnType.KVINNE),
                navn = listOf(lagNavn()),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(),
            )

        private fun fullmakter(): List<Fullmakt> =
            listOf(
                Fullmakt(
                    gyldigTilOgMed = startdato,
                    gyldigFraOgMed = sluttdato,
                    motpartsPersonident = "11111133333",
                    motpartsRolle = MotpartsRolle.FULLMEKTIG,
                    omraader = listOf(),
                ),
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> {
            return listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                    VergeEllerFullmektig(
                        motpartsPersonident = annenForelderFnr,
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
                        motpartsPersonident = annenForelderFnr,
                        identifiserendeInformasjon = null,
                        omfang = "personligeOgOekonomiskeInteresser",
                        omfangetErInnenPersonligOmraade = false,
                    ),
                ),
            )
        }
    }
}
