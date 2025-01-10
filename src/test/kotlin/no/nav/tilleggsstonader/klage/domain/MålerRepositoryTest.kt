package no.nav.tilleggsstonader.klage.metrics.domain

import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.temporal.IsoFields

internal class MålerRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var målerRepository: MålerRepository

    val år = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR)
    val uke = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    @BeforeEach
    internal fun setUp() {
        val fagsak1 = fagsak()
        testoppsettService.lagreFagsak(fagsak1)
        testoppsettService.lagreBehandling(
            behandling(
                fagsak1,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.MEDHOLD,
            ),
        )
        testoppsettService.lagreBehandling(
            behandling(
                fagsak1,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD,
            ),
        )
        testoppsettService.lagreBehandling(
            behandling(
                fagsak1,
                status = BehandlingStatus.OPPRETTET,
                resultat = BehandlingResultat.IKKE_SATT,
            ),
        )

        val fagsak2 = fagsak(identer = setOf(PersonIdent("123")), stønadstype = Stønadstype.BARNETILSYN)
        testoppsettService.lagreFagsak(fagsak2)
        testoppsettService.lagreBehandling(
            behandling(
                fagsak2,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.MEDHOLD,
            ),
        )
        testoppsettService.lagreBehandling(
            behandling(
                fagsak2,
                status = BehandlingStatus.VENTER,
                resultat = BehandlingResultat.MEDHOLD,
            ),
        )
    }

    @Test
    internal fun finnBehandlingerPerStatus() {
        val data = målerRepository.finnBehandlingerPerStatus()

        assertThat(data).containsExactlyInAnyOrder(
            BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.FERDIGSTILT, 3),
            BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.OPPRETTET, 1),
            BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.VENTER, 1),
        )
    }

    @Test
    internal fun finnÅpneBehandlingerPerUke() {
        val data = målerRepository.finnÅpneBehandlingerPerUke()

        assertThat(data).containsExactlyInAnyOrder(
            ÅpneBehandlingerFraUke(år, uke, Stønadstype.BARNETILSYN, 1),
        )
    }

    @Test
    internal fun finnVedtakPerUke() {
        val data = målerRepository.antallVedtak()

        assertThat(data).containsExactlyInAnyOrder(
            AntallVedtak(Stønadstype.BARNETILSYN, BehandlingResultat.MEDHOLD, 3),
            AntallVedtak(Stønadstype.BARNETILSYN, BehandlingResultat.IKKE_MEDHOLD, 1),
        )
    }
}
