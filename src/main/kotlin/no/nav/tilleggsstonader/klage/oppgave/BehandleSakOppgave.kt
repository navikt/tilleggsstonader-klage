package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded

data class BehandleSakOppgave(
    @Id
    val behandlingId: BehandlingId,
    val oppgaveId: Long,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
