package no.nav.tilleggsstonader.klage.journalpost

import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return familieIntegrasjonerClient.hentJournalpost(journalpostId)
    }

    fun finnJournalposter(
        personIdent: String,
        stønadType: Stønadstype,
        antall: Int = 200,
        typer: List<Journalposttype> = Journalposttype.values().toList(),
    ): List<Journalpost> {
        return familieIntegrasjonerClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId = Bruker(
                    id = personIdent,
                    type = BrukerIdType.FNR,
                ),
                antall = antall,
                tema = listOf(stønadType.tilTema()),
                journalposttype = typer,
            ),
        )
    }

    fun hentDokument(
        journalpost: Journalpost,
        dokumentInfoId: String,
    ): ByteArray {
        validerDokumentKanHentes(journalpost, dokumentInfoId)
        return familieIntegrasjonerClient.hentDokument(journalpost.journalpostId, dokumentInfoId)
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String,
    ) {
        val dokument = journalpost.dokumenter?.find { it.dokumentInfoId == dokumentInfoId }
        feilHvis(dokument == null) {
            "Finner ikke dokument med $dokumentInfoId for journalpost=${journalpost.journalpostId}"
        }
        brukerfeilHvisIkke(dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ARKIV } ?: false) {
            "Vedlegget er sannsynligvis under arbeid, må åpnes i gosys"
        }
    }
}
