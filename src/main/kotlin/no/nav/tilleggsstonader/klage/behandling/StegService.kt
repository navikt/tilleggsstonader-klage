package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlerRolle
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val tilgangService: TilgangService,
) {
    @Transactional
    fun oppdaterSteg(
        behandlingId: BehandlingId,
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerHarSaksbehandlerRolle(behandlingId)
        validerGyldigNesteSteg(behandling)
        oppdaterBehandlingOgHistorikk(behandling.id, nåværendeSteg, nesteSteg, behandlingsresultat)
    }

    private fun oppdaterBehandlingOgHistorikk(
        behandlingId: BehandlingId,
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
    ) {
        behandlingRepository.updateSteg(behandlingId, nesteSteg)
        behandlingRepository.updateStatus(behandlingId, nesteSteg.gjelderStatus)

        if (skalOppretteHistorikkradForNåværendeSteg(nåværendeSteg, nesteSteg, behandlingsresultat)) {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, nåværendeSteg)
        }
        if (nesteSteg == StegType.KABAL_VENTER_SVAR) {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.OVERFØRING_TIL_KABAL)
        }
        if (nesteSteg == StegType.BEHANDLING_FERDIGSTILT) {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.BEHANDLING_FERDIGSTILT)
        }
    }

    private fun skalOppretteHistorikkradForNåværendeSteg(
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
    ) = !(
        nåværendeSteg == StegType.BREV &&
            nesteSteg == StegType.BEHANDLING_FERDIGSTILT &&
            behandlingsresultat == BehandlingResultat.MEDHOLD
    )

    private fun validerGyldigNesteSteg(behandling: Behandling) =
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Behandlingen er låst for videre behandling"
        }

    private fun validerHarSaksbehandlerRolle(behandlingId: BehandlingId) =
        feilHvisIkke(
            tilgangService.harTilgangTilBehandlingGittRolle(behandlingId, BehandlerRolle.SAKSBEHANDLER),
        ) { "Saksbehandler har ikke tilgang til å oppdatere behandlingssteg" }
}
