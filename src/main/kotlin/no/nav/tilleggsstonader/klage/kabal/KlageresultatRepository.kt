package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KlageresultatRepository : RepositoryInterface<KlageinstansResultat, UUID>, InsertUpdateRepository<KlageinstansResultat> {

    fun findByBehandlingId(behandlingId: UUID): List<KlageinstansResultat>
}
