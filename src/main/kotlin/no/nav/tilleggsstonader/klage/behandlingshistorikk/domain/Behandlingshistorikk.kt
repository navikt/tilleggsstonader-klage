package no.nav.tilleggsstonader.klage.behandlingshistorikk.domain

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.SporbarUtils
import no.nav.tilleggsstonader.klage.infrastruktur.repository.JsonWrapper
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val steg: StegType,
    val utfall: StegUtfall?,
    val metadata: JsonWrapper? = null,
    val opprettetAvNavn: String? = null,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime? = SporbarUtils.now(),
    val gitVersjon: String?,
)

enum class StegUtfall {
    HENLAGT,
    SATT_PÅ_VENT,
    TATT_AV_VENT,
}
