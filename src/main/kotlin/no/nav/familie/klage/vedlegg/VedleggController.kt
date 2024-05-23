package no.nav.tilleggsstonader.klage.vedlegg

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.klage.journalpost.JournalpostService
import no.nav.tilleggsstonader.klage.personopplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vedlegg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedleggController(
    private val vedleggService: VedleggService,
    private val tilgangService: TilgangService,
    private val journalpostService: JournalpostService,
    private val pdlClient: PdlClient,
) {

    @GetMapping("/{behandlingId}")
    fun finnVedleggForBehandling(@PathVariable behandlingId: UUID): Ressurs<List<DokumentinfoDto>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(vedleggService.finnVedleggPåBehandling(behandlingId))
    }

    @GetMapping("/{journalpostId}/dokument-pdf/{dokumentInfoId}", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun hentDokumentSomPdf(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: String): ByteArray {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedRelasjoner(personIdent, AuditLoggerEvent.ACCESS)
        return journalpostService.hentDokument(journalpost, dokumentInfoId)
    }

    private fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val personIdent = journalpost.bruker?.let {
            when (it.idType) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> pdlClient.hentPersonidenter(it.id, Tema.valueOf(journalpost.tema ?: error("Tema er null for journalpostId=$journalpostId"))).identer.first().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=$journalpostId for orgnr")
                else -> {}
            }
        } ?: error("Kan ikke hente journalpost=$journalpostId uten bruker")
        return Pair(journalpost, personIdent)
    }
}
