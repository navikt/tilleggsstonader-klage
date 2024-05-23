package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.OpprettRevurderingUtil.skalOppretteRevurderingAutomatisk
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OpprettRevurderingService(
    private val behandlingService: BehandlingService,
    private val fagsystemVedtakService: FagsystemVedtakService,
) {

    fun kanOppretteRevurdering(behandlingId: UUID): KanOppretteRevurderingResponse {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return if (skalOppretteRevurderingAutomatisk(behandling.påklagetVedtak)) {
            fagsystemVedtakService.kanOppretteRevurdering(behandlingId)
        } else {
            KanOppretteRevurderingResponse(false, null)
        }
    }
}
