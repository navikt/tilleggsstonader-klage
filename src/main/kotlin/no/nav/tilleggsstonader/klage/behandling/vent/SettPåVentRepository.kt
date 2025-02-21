package no.nav.tilleggsstonader.klage.behandling.vent

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SettPåVentRepository :
    RepositoryInterface<SettPåVent, UUID>,
    InsertUpdateRepository<SettPåVent> {
    fun findByBehandlingIdAndAktivIsTrue(behandlingId: BehandlingId): SettPåVent?
}
