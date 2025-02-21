package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface BehandleSakOppgaveRepository :
    RepositoryInterface<BehandleSakOppgave, BehandlingId>,
    InsertUpdateRepository<BehandleSakOppgave> {
    fun findByBehandlingId(behandlingId: BehandlingId): BehandleSakOppgave

    @Query(
        """
        SELECT * from behandle_sak_oppgave WHERE oppgave_id IN (:oppgaveIder)
    """,
    )
    fun finnForOppgaveIder(oppgaveIder: List<Long>): List<BehandleSakOppgave>
}
