package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.domain.Avsnitt
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AvsnittRepository :
    RepositoryInterface<Avsnitt, UUID>,
    InsertUpdateRepository<Avsnitt> {
    fun findByBehandlingId(behandlingId: BehandlingId): List<Avsnitt>

    @Modifying
    @Query("""DELETE FROM avsnitt WHERE avsnitt.behandling_id = :behandlingId""")
    fun slettAvsnittMedBehandlingId(behandlingId: BehandlingId)
}
