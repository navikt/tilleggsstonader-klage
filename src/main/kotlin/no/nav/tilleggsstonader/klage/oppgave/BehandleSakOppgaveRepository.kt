package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandleSakOppgaveRepository :
    RepositoryInterface<BehandleSakOppgave, UUID>,
    InsertUpdateRepository<BehandleSakOppgave> {
    fun findByBehandlingId(behandlingId: UUID): BehandleSakOppgave

    @Query(
        """
        SELECT * from behandle_sak_oppgave WHERE oppgave_id IN (:oppgaveIder)
    """,
    )
    fun finnForOppgaveIder(oppgaveIder: List<Long>): List<BehandleSakOppgave>
}
