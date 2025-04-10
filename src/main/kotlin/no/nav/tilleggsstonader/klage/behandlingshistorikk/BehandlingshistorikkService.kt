package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.JsonWrapper
import no.nav.tilleggsstonader.klage.infrastruktur.repository.tilJson
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
                metadata = metadata?.let { wrapper(it) },
                opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                gitVersjon = Applikasjonsversjon.versjon,
            ),
        )

    fun sattPåVent(
        behandling: Behandling,
        kommentar: String?,
        årsaker: List<ÅrsakSettPåVent>,
    ) {
        val metadata: MutableMap<String, Any> = mutableMapOf("årsaker" to årsaker)
        kommentar?.let { metadata["kommentarSettPåVent"] = it }

        opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            utfall = StegUtfall.SATT_PÅ_VENT,
            metadata = metadata,
        )
    }

    fun taAvVent(
        behandling: Behandling,
        kommentar: String?,
    ) {
        opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            utfall = StegUtfall.TATT_AV_VENT,
            metadata = kommentar?.takeIf { it.isNotEmpty() }?.let { mapOf("kommentar" to it) },
        )
    }

    fun slettFritekstMetadataVedFerdigstillelse(behandlingId: BehandlingId) {
        val relevanteHistorikkinnslag = hentBehandlingshistorikk(behandlingId = behandlingId)

        relevanteHistorikkinnslag.forEach { historikkinnslag ->
            when (historikkinnslag.utfall) {
                StegUtfall.SATT_PÅ_VENT,
                StegUtfall.TATT_AV_VENT,
                -> {
                    val json = historikkinnslag.metadata?.tilJson()?.toMutableMap()
                    if (json != null && (json.contains("kommentar") || json.contains("kommentarSettPåVent"))) {
                        json.remove("kommentar")
                        json.remove("kommentarSettPåVent")
                        val oppdatertHistorikkinnslag = historikkinnslag.copy(metadata = wrapper(json))
                        behandlingshistorikkRepository.update(oppdatertHistorikkinnslag)
                    }
                }
                else -> {}
            }
        }
    }

    private fun wrapper(obj: Any): JsonWrapper = JsonWrapper(objectMapper.writeValueAsString(obj))
}
