package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>, InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>
}
