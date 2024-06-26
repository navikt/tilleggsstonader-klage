package no.nav.tilleggsstonader.klage.testutil

import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Dødsfall
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Fullmakt
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Kjønn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.KjønnType
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Metadata
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Navn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlNavn
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlSøker
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt

object PdlTestdataHelper {

    val metadataGjeldende = Metadata(historisk = false)

    fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = Kjønn(kjønnType)

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
        historisk: Boolean = false,
    ): Navn {
        return Navn(
            fornavn,
            mellomnavn,
            etternavn,
            Metadata(historisk = historisk),
        )
    }

    fun pdlNavn(
        navn: List<Navn> = emptyList(),
    ) =
        PdlNavn(
            navn,
        )

    fun pdlSøker(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
        fullmakt: List<Fullmakt> = emptyList(),
        kjønn: Kjønn? = null,
        navn: List<Navn> = emptyList(),
        vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList(),
    ) =
        PdlSøker(
            adressebeskyttelse,
            dødsfall,
            listOfNotNull(kjønn),
            folkeregisterpersonstatus,
            fullmakt,
            navn,
            vergemaalEllerFremtidsfullmakt,
        )
}
