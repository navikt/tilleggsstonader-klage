package no.nav.tilleggsstonader.klage.personopplysninger.pdl

fun Navn.visningsnavn(): String {
    return if (mellomnavn == null) {
        "$fornavn $etternavn"
    } else {
        "$fornavn $mellomnavn $etternavn"
    }
}

fun Personnavn.visningsnavn(): String {
    return if (mellomnavn == null) {
        "$fornavn $etternavn"
    } else {
        "$fornavn $mellomnavn $etternavn"
    }
}

fun List<Navn>.gjeldende(): Navn = this.single()
fun List<Dødsfall>.gjeldende(): Dødsfall? = this.firstOrNull()
fun List<Folkeregisterpersonstatus>.gjeldende(): Folkeregisterpersonstatus? = this.find { !it.metadata.historisk }
fun List<Adressebeskyttelse>.gjeldende(): Adressebeskyttelse? = this.find { !it.metadata.historisk }

fun PdlIdenter.identer(): Set<String> = this.identer.map { it.ident }.toSet()
