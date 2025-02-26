package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.infrastruktur.config.LenkeConfig
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtKlager
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtPartId
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtPartIdType
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtSak
import no.nav.tilleggsstonader.klage.kabal.domain.Type
import no.nav.tilleggsstonader.klage.kabal.domain.Ytelse
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val integrasjonerClient: TilleggsstønaderIntegrasjonerClient,
    private val lenkeConfig: LenkeConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendTilKabal(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlerIdent: String,
    ) {
        val oversendtKlageAnkeV3 = lagKlageOversendelseV3(fagsak, behandling, vurdering, hentSaksbehandlersEnhet(saksbehandlerIdent))
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    private fun hentSaksbehandlersEnhet(saksbehandlerIdent: String): String {
        try {
            val tilleggsstønaderInnEnhet = integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent)
            return tilleggsstønaderInnEnhet.enhet
        } catch (e: Exception) {
            logger.error("Feilet uthenting av enhet for saksbehandler. Se secure logs for detaljer")
            secureLogger.error("feilet ved uthenting av enhet for NAV-ident: $saksbehandlerIdent", e)
            return "4462" // fallback til virtuell arbedisbenk om uthentig av den ansattes enhet feiler
        }
    }

    private fun lagKlageOversendelseV3(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlersEnhet: String,
    ): OversendtKlageAnkeV3 =
        OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager =
                OversendtKlager(
                    id =
                        OversendtPartId(
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
            ytelse = mapYtelse(fagsak),
        )

    private fun mapYtelse(fagsak: Fagsak): Ytelse =
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN -> Ytelse.TSO_TSO
            Stønadstype.LÆREMIDLER -> Ytelse.TSO_TSO
            Stønadstype.BOUTGIFTER -> Ytelse.TSO_TSO
            else -> error("Har ikke lagt til mapping mellom ${fagsak.stønadstype} og ytelse")
        }

    private fun lagInnsynUrl(
        fagsak: Fagsak,
        påklagetVedtak: PåklagetVedtak,
    ): String {
        val fagsystemUrl = lenkeConfig.tilleggsstonaderSakLenke
        val påklagetVedtakDetaljer = påklagetVedtak.påklagetVedtakDetaljer

        return if (påklagetVedtakDetaljer?.eksternFagsystemBehandlingId != null) {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}"
        } else {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
        }
    }
}
