package no.nav.tilleggsstonader.klage.personopplysninger.dto

import java.time.LocalDate
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: String,
    val adressebeskyttelse: Adressebeskyttelse?,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val dødsdato: LocalDate?,
    val egenAnsatt: Boolean,
    val harFullmektig: Boolean,
    val vergemål: List<VergemålDto>,
)

@Suppress("unused") // Kopi fra PDL
enum class Adressebeskyttelse {

    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    fun erStrengtFortrolig() = this == STRENGT_FORTROLIG || this == STRENGT_FORTROLIG_UTLAND
}

@Suppress("unused") // Kopi fra PDL
enum class Kjønn {

    KVINNE,
    MANN,
    UKJENT,
}

data class VergemålDto(
    val embete: String?,
    val type: String?,
    val motpartsPersonident: String?,
    val navn: String?,
    val omfang: String?,
)

@Suppress("unused")
enum class Folkeregisterpersonstatus(private val pdlStatus: String) {

    BOSATT("bosatt"),
    UTFLYTTET("utflyttet"),
    FORSVUNNET("forsvunnet"),
    DØD("doed"),
    OPPHØRT("opphoert"),
    FØDSELSREGISTRERT("foedselsregistrert"),
    MIDLERTIDIG("midlertidig"),
    INAKTIV("inaktiv"),
    UKJENT("ukjent"),
    ;

    companion object {

        private val map = entries.associateBy(Folkeregisterpersonstatus::pdlStatus)
        fun fraPdl(status: PdlFolkeregisterpersonstatus) = map.getOrDefault(status.status, UKJENT)
    }
}
