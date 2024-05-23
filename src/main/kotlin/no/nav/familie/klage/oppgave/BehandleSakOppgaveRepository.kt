package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandleSakOppgaveRepository :
    RepositoryInterface<BehandleSakOppgave, UUID>,
    InsertUpdateRepository<BehandleSakOppgave> {
    fun findByBehandlingId(behandlingId: UUID): BehandleSakOppgave
}
