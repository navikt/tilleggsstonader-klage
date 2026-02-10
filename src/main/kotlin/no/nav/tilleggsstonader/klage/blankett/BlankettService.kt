package no.nav.tilleggsstonader.klage.blankett

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.brev.FamilieDokumentClient
import no.nav.tilleggsstonader.klage.brev.HtmlifyClient
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.formkrav.dto.FormkravDto
import no.nav.tilleggsstonader.klage.personopplysninger.PersonopplysningerService
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto
import no.nav.tilleggsstonader.klage.vurdering.dto.tilHjemler
import org.springframework.stereotype.Service

@Service
class BlankettService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val personopplysningerService: PersonopplysningerService,
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val htmlifyClient: HtmlifyClient,
    private val familieDokumentClient: FamilieDokumentClient,
) {
    fun lagBlankett(behandlingId: BehandlingId): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val formkrav = formService.hentFormDto(behandlingId)
        val vurdering = vurderingService.hentVurderingDto(behandlingId)
        val påklagetVedtak = mapPåklagetVedtak(behandling)

        val blankettPdfRequest =
            BlankettPdfRequest(
                behandling =
                    BlankettPdfBehandling(
                        eksternFagsakId = fagsak.eksternId,
                        stønadstype = fagsak.stønadstype,
                        klageMottatt = behandling.klageMottatt,
                        resultat = behandling.resultat,
                        påklagetVedtak = påklagetVedtak,
                    ),
                personopplysninger = lagPersonopplysningerDto(behandling, fagsak),
                formkrav = mapFormkrav(formkrav),
                vurdering = mapVurdering(vurdering),
            )
        val blankett = htmlifyClient.genererBlankett(blankettPdfRequest)
        return familieDokumentClient.genererPdfFraHtml(blankett)
    }

    private fun mapPåklagetVedtak(behandling: Behandling): BlankettPåklagetVedtakDto? =
        behandling.påklagetVedtak.påklagetVedtakDetaljer?.let { påklagetVedtakDetaljer ->
            BlankettPåklagetVedtakDto(
                behandlingstype = påklagetVedtakDetaljer.behandlingstype,
                resultat = påklagetVedtakDetaljer.resultat,
                vedtakstidspunkt = påklagetVedtakDetaljer.vedtakstidspunkt,
            )
        }

    private fun mapVurdering(vurdering: VurderingDto?): BlankettVurderingDto? =
        vurdering?.let {
            BlankettVurderingDto(
                vedtak = it.vedtak,
                årsak = it.årsak,
                begrunnelseOmgjøring = it.begrunnelseOmgjøring,
                hjemler = it.hjemler?.tilHjemler(),
                innstillingKlageinstans = it.innstillingKlageinstans,
                interntNotat = it.interntNotat,
            )
        }

    private fun mapFormkrav(formkrav: FormkravDto) =
        BlankettFormDto(
            klagePart = formkrav.klagePart,
            klageKonkret = formkrav.klageKonkret,
            klagefristOverholdt = formkrav.klagefristOverholdt,
            klagefristOverholdtUnntak = formkrav.klagefristOverholdtUnntak,
            klageSignert = formkrav.klageSignert,
            saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
            brevtekst = formkrav.brevtekst,
        )

    private fun lagPersonopplysningerDto(
        behandling: Behandling,
        fagsak: Fagsak,
    ): PersonopplysningerDto {
        val personIdent = fagsak.hentAktivIdent()
        val navn = personopplysningerService.hentPersonopplysninger(behandling.id).navn
        return PersonopplysningerDto(navn, personIdent)
    }
}
