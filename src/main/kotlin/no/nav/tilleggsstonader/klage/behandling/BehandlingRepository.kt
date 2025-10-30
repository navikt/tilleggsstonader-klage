package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.infrastruktur.repository.RepositoryInterface
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository :
    RepositoryInterface<Behandling, BehandlingId>,
    InsertUpdateRepository<Behandling> {
    @Modifying
    @Query(
        """UPDATE behandling SET steg = :steg WHERE id = :behandlingId""",
    )
    fun updateSteg(
        behandlingId: BehandlingId,
        steg: StegType,
    )

    @Modifying
    @Query(
        """UPDATE behandling SET status = :nyStatus WHERE id = :behandling_id""",
    )
    fun updateStatus(
        @Param("behandling_id") behandlingId: BehandlingId,
        nyStatus: BehandlingStatus,
    )

    fun findByEksternBehandlingId(eksternBehandlingId: UUID): Behandling

    @Query(
        """
            SELECT
            DISTINCT
             b.id,
             b.fagsak_id,
             f.fagsak_person_id,
             b.status,
             b.opprettet_tid opprettet,
             b.klage_mottatt mottatt_dato,
             b.resultat,
             v.arsak,
             b.vedtak_dato vedtaksdato,
             b.henlagt_arsak,
             b.henlagt_begrunnelse,
             FIRST_VALUE(ident) OVER (PARTITION BY pi.fagsak_person_id ORDER BY pi.endret_tid DESC) AS ident
            FROM behandling b
            JOIN fagsak f ON f.id = b.fagsak_id
            JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
            LEFT JOIN vurdering v ON v.behandling_id = b.id
            WHERE f.ekstern_id = :eksternFagsakId AND f.fagsystem = :fagsystem
        """,
    )
    fun finnKlagebehandlingsresultat(
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ): List<Klagebehandlingsresultat>

    fun findByFagsakId(fagsakId: UUID): List<Behandling>
}
