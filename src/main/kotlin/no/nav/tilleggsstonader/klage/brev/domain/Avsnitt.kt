package no.nav.tilleggsstonader.klage.brev.domain

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Avsnitt(
    @Id
    val avsnittId: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val deloverskrift: String,
    val innhold: String,
    @Column("skal_skjules_i_brevbygger")
    val skalSkjulesIBrevbygger: Boolean? = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
