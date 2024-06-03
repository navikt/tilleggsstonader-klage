package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.fagsak.domain.tilYtelse
import no.nav.tilleggsstonader.klage.infrastruktur.config.LenkeConfig
import no.nav.tilleggsstonader.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val integrasjonerClient: FamilieIntegrasjonerClient,
    private val lenkeConfig: LenkeConfig,
) {

    fun sendTilKabal(fagsak: Fagsak, behandling: Behandling, vurdering: Vurdering, saksbehandlerIdent: String) {
        val saksbehandler = integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent)
        val oversendtKlageAnkeV3 = lagKlageOversendelseV3(fagsak, behandling, vurdering, saksbehandler.enhet)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    private fun lagKlageOversendelseV3(fagsak: Fagsak, behandling: Behandling, vurdering: Vurdering, saksbehandlersEnhet: String): OversendtKlageAnkeV3 {
        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = OversendtKlager(
                id = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    verdi = fagsak.hentAktivIdent(),
                ),
            ),
            fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem),
            kildeReferanse = behandling.eksternBehandlingId.toString(),
            innsynUrl = lagInnsynUrl(fagsak, behandling.påklagetVedtak),
            hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
            forrigeBehandlendeEnhet = saksbehandlersEnhet,
            tilknyttedeJournalposter = listOf(),
            brukersHenvendelseMottattNavDato = behandling.klageMottatt,
            innsendtTilNav = behandling.klageMottatt,
            kilde = fagsak.fagsystem,
            ytelse = fagsak.stønadstype.tilYtelse(),
        )
    }

    private fun lagInnsynUrl(fagsak: Fagsak, påklagetVedtak: PåklagetVedtak): String {
        val fagsystemUrl = when (fagsak.fagsystem) {
            Fagsystem.TILLEGGSSTONADER -> lenkeConfig.efSakLenke // TODO: Bytt ut lenke
        }
        val påklagetVedtakDetaljer = påklagetVedtak.påklagetVedtakDetaljer
        return if (påklagetVedtakDetaljer?.eksternFagsystemBehandlingId != null) {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}"
        } else {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
        }
    }
}