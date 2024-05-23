package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.distribusjon.DokumenttypeUtil.dokumenttypeBrev
import no.nav.tilleggsstonader.klage.distribusjon.DokumenttypeUtil.dokumenttypeSaksbehandlingsblankett
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import no.nav.tilleggsstonader.klage.felles.util.TekstUtil.storForbokstav
import no.nav.tilleggsstonader.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.tilleggsstonader.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.klage.BehandlingResultat
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DistribusjonService(
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {

    fun journalførBrev(
        behandlingId: UUID,
        brev: ByteArray,
        saksbehandler: String,
        index: Int = 0,
        mottaker: AvsenderMottaker,
    ): String {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        return journalfør(
            behandlingId = behandlingId,
            fagsak = fagsak,
            pdf = brev,
            tittel = utledBrevtittel(behandlingId),
            dokumenttype = dokumenttypeBrev(fagsak.stønadstype),
            saksbehandler = saksbehandler,
            suffixEksternReferanseId = "-$index",
            avsenderMottaker = mottaker,
        )
    }

    fun journalførSaksbehandlingsblankett(
        behandlingId: UUID,
        saksbehandlingsblankettPdf: ByteArray,
        saksbehandler: String,
    ): String {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        return journalfør(
            behandlingId = behandlingId,
            fagsak = fagsak,
            pdf = saksbehandlingsblankettPdf,
            tittel = "Blankett for klage på ${fagsak.stønadstype.name.storForbokstav()}",
            dokumenttype = dokumenttypeSaksbehandlingsblankett(fagsak.stønadstype),
            saksbehandler = saksbehandler,
            suffixEksternReferanseId = "-blankett",
        )
    }

    private fun journalfør(
        behandlingId: UUID,
        fagsak: Fagsak,
        pdf: ByteArray,
        tittel: String,
        dokumenttype: Dokumenttype,
        saksbehandler: String,
        suffixEksternReferanseId: String = "",
        avsenderMottaker: AvsenderMottaker? = null,
    ): String {
        val behandling = behandlingService.hentBehandling(behandlingId)

        val dokument = lagDokument(
            pdf = pdf,
            dokumenttype = dokumenttype,
            tittel = tittel,
        )
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = fagsak.hentAktivIdent(),
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsak.eksternId,
            journalførendeEnhet = behandling.behandlendeEnhet,
            eksternReferanseId = "${behandling.eksternBehandlingId}$suffixEksternReferanseId",
            avsenderMottaker = avsenderMottaker,
        )

        return familieIntegrasjonerClient.arkiverDokument(
            arkiverDokumentRequest,
            saksbehandler,
        ).journalpostId
    }

    fun distribuerBrev(journalpostId: String): String {
        return familieIntegrasjonerClient.distribuerBrev(journalpostId, Distribusjonstype.ANNET)
    }

    private fun lagDokument(
        pdf: ByteArray,
        dokumenttype: Dokumenttype,
        tittel: String,
    ): Dokument {
        return Dokument(
            dokument = pdf,
            filtype = Filtype.PDFA,
            filnavn = null,
            tittel = tittel,
            dokumenttype = dokumenttype,
        )
    }

    private fun utledBrevtittel(behandlingId: UUID): String {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype

        val tittelPrefix = when (behandling.resultat) {
            BehandlingResultat.IKKE_MEDHOLD -> "Brev om oversendelse til NAV Klageinstans"
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST -> "Vedtak om avvist klage"
            else -> error("Kan ikke utlede brevtittel for behandlingsresultat ${behandling.resultat}")
        }

        return "$tittelPrefix - ${stønadstype.visningsnavn()}"
    }
}
