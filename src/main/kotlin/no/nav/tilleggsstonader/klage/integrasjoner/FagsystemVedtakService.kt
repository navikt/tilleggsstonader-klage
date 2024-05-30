package no.nav.tilleggsstonader.klage.integrasjoner

import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.IkkeOpprettet
import no.nav.tilleggsstonader.kontrakter.klage.IkkeOpprettetÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

private val ukjentFeilVedOpprettRevurdering = OpprettRevurderingResponse(
    IkkeOpprettet(
        IkkeOpprettetÅrsak.FEIL,
        "Ukjent feil ved opprettelse av revurdering",
    ),
)

@Service
class FagsystemVedtakService(
    private val TSSakClient: TSSakClient,
    private val fagsakService: FagsakService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun hentFagsystemVedtak(behandlingId: UUID): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return hentFagsystemVedtak(fagsak)
    }

    private fun hentFagsystemVedtak(fagsak: Fagsak): List<FagsystemVedtak> = when (fagsak.fagsystem) {
        Fagsystem.TILLEGGSSTONADER -> TSSakClient.hentVedtak(fagsak.eksternId)
    }

    fun hentFagsystemVedtakForPåklagetBehandlingId(
        behandlingId: UUID,
        påklagetBehandlingId: String,
    ): FagsystemVedtak =
        hentFagsystemVedtak(behandlingId)
            .singleOrNull { it.eksternBehandlingId == påklagetBehandlingId }
            ?: error("Finner ikke vedtak for behandling=$behandlingId eksternBehandling=$påklagetBehandlingId")

    fun kanOppretteRevurdering(behandlingId: UUID): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (fagsak.fagsystem) {
            Fagsystem.TILLEGGSSTONADER -> TSSakClient.kanOppretteRevurdering(fagsak.eksternId)
        }
    }

    fun opprettRevurdering(behandlingId: UUID): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return try {
            when (fagsak.fagsystem) {
                Fagsystem.TILLEGGSSTONADER -> TSSakClient.opprettRevurdering(fagsak.eksternId)
            }
        } catch (e: Exception) {
            val errorSuffix = "Feilet opprettelse av revurdering for behandling=$behandlingId eksternFagsakId=${fagsak.eksternId}"
            logger.warn("$errorSuffix, se detaljer i secureLogs")
            secureLogger.warn(errorSuffix, e)

            ukjentFeilVedOpprettRevurdering
        }
    }
}
