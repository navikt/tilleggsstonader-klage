package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VurderingRepository : RepositoryInterface<Vurdering, UUID>, InsertUpdateRepository<Vurdering>
