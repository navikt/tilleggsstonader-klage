package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VurderingRepository : RepositoryInterface<Vurdering, UUID>, InsertUpdateRepository<Vurdering>
