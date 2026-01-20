package no.nav.tilleggsstonader.klage.journalpost

import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.kontrakter.felles.Arkivtema
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import org.springframework.stereotype.Service

@Service
class JournalpostService(
    private val tilleggsstønaderIntegrasjonerClient: TilleggsstønaderIntegrasjonerClient,
) {
    fun finnJournalposter(
        personIdent: String,
        antall: Int = 200,
        typer: List<Journalposttype> = Journalposttype.entries,
    ): List<Journalpost> =
        tilleggsstønaderIntegrasjonerClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId =
                    Bruker(
                        id = personIdent,
                        type = BrukerIdType.FNR,
                    ),
                antall = antall,
                tema = listOf(Arkivtema.TSO, Arkivtema.TSR),
                journalposttype = typer,
            ),
        )
}
