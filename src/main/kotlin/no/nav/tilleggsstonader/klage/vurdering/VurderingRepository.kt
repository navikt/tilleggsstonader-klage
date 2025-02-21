package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Repository

@Repository
interface VurderingRepository :
    RepositoryInterface<Vurdering, BehandlingId>,
    InsertUpdateRepository<Vurdering>
