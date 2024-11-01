package no.nav.tilleggsstonader.klage.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstonaderSakClient
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Dødsfall
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Fullmakt
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.IdentifiserendeInformasjon
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Navn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlNavn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Personnavn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.defaultIdenter
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.klage.testutil.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Adressebeskyttelse as PdlAdressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.AdressebeskyttelseGradering as PdlAdressebeskyttelseGradering1
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus1

internal class PersonopplysningerServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val pdlClient = mockk<PdlClient>()
    private val tilleggsstonaderSakClient = mockk<TilleggsstonaderSakClient>()

    private val personopplysningerService =
        PersonopplysningerService(behandlingService, fagsakService, pdlClient, tilleggsstonaderSakClient)

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { pdlClient.hentPerson(any(), any()) } returns lagPdlSøker()
        every { pdlClient.hentNavnBolk(any(), any()) } returns navnBolkResponse()
        every { tilleggsstonaderSakClient.erEgenAnsatt(any()) } returns true
    }

    @Test
    internal fun `skal mappe til dto`() {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandling.id)

        assertThat(personopplysninger.personIdent).isEqualTo(defaultIdenter.single().ident)
        assertThat(personopplysninger.navn).isEqualTo("Fornavn mellomnavn Etternavn")
        assertThat(personopplysninger.folkeregisterpersonstatus).isEqualTo(Folkeregisterpersonstatus.DØD)
        assertThat(personopplysninger.adressebeskyttelse).isEqualTo(Adressebeskyttelse.FORTROLIG)
        assertThat(personopplysninger.dødsdato).isEqualTo(LocalDate.now())
        assertThat(personopplysninger.fullmakt).hasSize(1)
        assertThat(personopplysninger.egenAnsatt).isTrue
        assertThat(personopplysninger.vergemål).hasSize(1)

        verify(exactly = 1) { pdlClient.hentNavnBolk(eq(listOf("fullmaktIdent")), any()) }
    }

    @Test
    internal fun `skal hente navn til fullmakt`() {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandling.id)

        assertThat(personopplysninger.fullmakt.single().navn).isEqualTo("fullmakt etternavn")

        verify(exactly = 1) { pdlClient.hentNavnBolk(eq(listOf("fullmaktIdent")), any()) }
    }

    private fun navnBolkResponse() = mapOf(
        "fullmaktIdent" to PdlNavn(listOf(Navn("fullmakt", null, "etternavn", metadataGjeldende))),
    )

    private fun lagPdlSøker() = pdlSøker(
        listOf(PdlAdressebeskyttelse(PdlAdressebeskyttelseGradering1.FORTROLIG, metadataGjeldende)),
        listOf(Dødsfall(LocalDate.now())),
        listOf(PdlFolkeregisterpersonstatus1("doed", "d", metadataGjeldende)),
        listOf(Fullmakt(LocalDate.now(), LocalDate.now(), "fullmaktIdent", MotpartsRolle.FULLMEKTIG, listOf("o"))),
        listOf(lagNavn()),
        listOf(
            VergemaalEllerFremtidsfullmakt(
                "embete",
                null,
                "type",
                VergeEllerFullmektig(
                    IdentifiserendeInformasjon(Personnavn("", "", null)),
                    "vergeIdent",
                    "omfang",
                    true,
                ),
            ),
        ),
    )
}
