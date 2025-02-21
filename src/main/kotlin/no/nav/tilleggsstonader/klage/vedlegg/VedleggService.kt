package no.nav.tilleggsstonader.klage.vedlegg

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.journalpost.JournalpostService
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VedleggService(
    private val behandlingService: BehandlingService,
    private val journalpostService: JournalpostService,
) {
    fun finnVedleggPåBehandling(behandlingId: BehandlingId): List<DokumentinfoDto> {
        val (personIdent) = behandlingService.hentAktivIdent(behandlingId)
        val journalposter = journalpostService.finnJournalposter(personIdent)

        return journalposter
            .flatMap { journalpost ->
                journalpost.dokumenter?.map { tilDokumentInfoDto(it, journalpost) } ?: emptyList()
            }
    }

    private fun tilDokumentInfoDto(
        dokumentInfo: DokumentInfo,
        journalpost: Journalpost,
    ): DokumentinfoDto =
        DokumentinfoDto(
            dokumentinfoId = dokumentInfo.dokumentInfoId,
            filnavn = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.filnavn,
            tittel = dokumentInfo.tittel ?: "Tittel mangler",
            journalpostId = journalpost.journalpostId,
            dato = mestRelevanteDato(journalpost),
            journalstatus = journalpost.journalstatus,
            journalposttype = journalpost.journalposttype,
            logiskeVedlegg = dokumentInfo.logiskeVedlegg?.map { LogiskVedleggDto(tittel = it.tittel) } ?: emptyList(),
        )

    fun mestRelevanteDato(journalpost: Journalpost): LocalDateTime? =
        journalpost.datoMottatt
            ?: journalpost.relevanteDatoer?.maxByOrNull { datoTyperSortert(it.datotype) }?.dato

    private fun datoTyperSortert(datoType: String) =
        when (datoType) {
            "DATO_REGISTRERT" -> 4
            "DATO_JOURNALFOERT" -> 3
            "DATO_DOKUMENT" -> 2
            "DATO_OPPRETTET" -> 1
            else -> 0
        }
}
