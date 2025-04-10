package no.nav.tilleggsstonader.klage.behandling.vent

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("sett_pa_vent")
data class SettPåVent(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val oppgaveId: Long,
    @Column("arsaker")
    val årsaker: List<ÅrsakSettPåVent>,
    val kommentar: String?,
    val aktiv: Boolean = true,
    val taAvVentKommentar: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        feilHvis(årsaker.isEmpty()) {
            "SettPåVent må inneholde årsaker for behandling=$behandlingId"
        }
    }
}
