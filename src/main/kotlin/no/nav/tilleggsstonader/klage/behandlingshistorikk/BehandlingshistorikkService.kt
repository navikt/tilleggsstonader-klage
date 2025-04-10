package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.JsonWrapper
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.klage.util.Applikasjonsversjon
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
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
        utfall: StegUtfall? = null,
        metadata: Any? = null,
    ): Behandlingshistorikk =
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = steg,
                utfall = utfall,
                metadata = metadata?.let { JsonWrapper(objectMapper.writeValueAsString(it)) },
                opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                gitVersjon = Applikasjonsversjon.versjon,
            ),
        )
}
