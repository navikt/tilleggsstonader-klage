package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class VurderingRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var vurderingRepository: VurderingRepository

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    val behandlingId = BehandlingId.random()

    @BeforeEach
    fun setUp() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagreBehandling(behandling(fagsak, id = behandlingId))
    }

    @Test
    fun `skal kunne lagre og hente vurdering`() {
        val vurdering =
            vurdering(
                behandlingId = behandlingId,
                vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                årsak = Årsak.FEIL_PROSESSUELL,
                begrunnelseOmgjøring = "omgjøring",
                interntNotat = "internt notat",
            )
        vurderingRepository.insert(vurdering)

        val vurderingFraDb = vurderingRepository.findByIdOrThrow(vurdering.behandlingId)

        assertThat(vurderingFraDb)
            .usingRecursiveComparison()
            .ignoringFields("sporbar")
            .isEqualTo(vurdering)
        assertJsonVurdering()
    }

    private fun assertJsonVurdering() {
        val hjemlerJson =
            jdbcTemplate.query(
                "SELECT * FROM vurdering WHERE behandling_id = :behandlingId",
                mapOf("behandlingId" to behandlingId.id),
            ) { rs, _ -> rs.getString("hjemler") }
        assertThat(hjemlerJson).containsExactly("""["FS_TILL_ST_10_TILSYN"]""")
    }

    @Test
    fun `skal håndtere at hjemler ikke er lagret`() {
        val vurdering =
            vurdering(
                behandlingId = behandlingId,
                vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                hjemler = null,
                årsak = Årsak.FEIL_PROSESSUELL,
                begrunnelseOmgjøring = "omgjøring",
                interntNotat = "internt notat",
            )
        vurderingRepository.insert(vurdering)

        val vurderingFraDb = vurderingRepository.findByIdOrThrow(vurdering.behandlingId)

        assertThat(vurderingFraDb.hjemler).isNull()
        assertThat(vurderingFraDb)
            .usingRecursiveComparison()
            .ignoringFields("sporbar")
            .isEqualTo(vurdering)
    }
}
