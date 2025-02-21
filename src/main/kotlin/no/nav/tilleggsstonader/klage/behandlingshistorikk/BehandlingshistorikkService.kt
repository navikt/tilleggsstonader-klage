package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import org.springframework.stereotype.Service

@Service
class BehandlingshistorikkService(
    private val behandlingshistorikkRepository: BehandlingshistorikkRepository,
) {
    fun hentBehandlingshistorikk(behandlingId: BehandlingId): List<Behandlingshistorikk> =
        behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(behandlingId)

    fun opprettBehandlingshistorikk(
        behandlingId: BehandlingId,
        steg: StegType,
    ): Behandlingshistorikk =
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(behandlingId = behandlingId, steg = steg),
        )
}
