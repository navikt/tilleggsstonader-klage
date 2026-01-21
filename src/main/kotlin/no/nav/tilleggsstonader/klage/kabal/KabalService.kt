package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.infrastruktur.config.LenkeConfig
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtKlageAnkeV4
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtPartId
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtPartIdType
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtSak
import no.nav.tilleggsstonader.klage.kabal.domain.OversendtSakenGjelder
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
        val oversendtKlageAnkeV4 = lagKlageOversendelseV4(fagsak, behandling, vurdering, hentSaksbehandlersEnhet(saksbehandlerIdent))
        kabalClient.sendTilKabal(oversendtKlageAnkeV4)
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

    private fun lagKlageOversendelseV4(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlersEnhet: String,
    ): OversendtKlageAnkeV4 =
        OversendtKlageAnkeV4(
            type = Type.KLAGE,
            sakenGjelder =
                OversendtSakenGjelder(
                    id =
                        OversendtPartId(
                            type = OversendtPartIdType.PERSON,
                            verdi = fagsak.hentAktivIdent(),
                        ),
                ),
            fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem),
            kildeReferanse = behandling.eksternBehandlingId.toString(),
            hjemler = vurdering.hjemler?.hjemler?.map { it.kabalHjemmel } ?: emptyList(),
            forrigeBehandlendeEnhet = saksbehandlersEnhet,
            tilknyttedeJournalposter = listOf(),
            brukersKlageMottattVedtaksinstans = behandling.klageMottatt,
            ytelse = mapYtelse(fagsak),
        )

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private fun mapYtelse(fagsak: Fagsak): Ytelse =
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN -> Ytelse.TSO_TSO
            Stønadstype.LÆREMIDLER -> Ytelse.TSO_TSO
            Stønadstype.BOUTGIFTER -> Ytelse.TSO_TSO
            Stønadstype.DAGLIG_REISE_TSO -> Ytelse.TSO_TSO
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
