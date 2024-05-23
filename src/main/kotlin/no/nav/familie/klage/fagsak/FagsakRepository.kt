package no.nav.tilleggsstonader.klage.fagsak

import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.klage.Fagsystem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<FagsakDomain, UUID>, InsertUpdateRepository<FagsakDomain> {

    fun findByEksternIdAndFagsystemAndStønadstype(
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype,
    ): FagsakDomain?

    @Query(
        """SELECT f.*
                    FROM fagsak f
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""",
    )
    fun finnFagsakForBehandlingId(behandlingId: UUID): FagsakDomain?
}
