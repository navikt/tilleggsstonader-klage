package no.nav.tilleggsstonader.klage.repository

import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import kotlin.random.Random

internal class FagsakRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert =
            testoppsettService.lagreFagsak(
                fagsakDomain().tilFagsakMedPerson(
                    setOf(
                        PersonIdent("12345678901"),
                        PersonIdent("98765432109"),
                    ),
                ),
            )
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.id).isEqualTo(fagsakPersistert.id)
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

        testoppsettService.lagreFagsak(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val fagsakForBehandling = fagsakRepository.finnFagsakForBehandlingId(behandling.id)!!

        assertThat(fagsakForBehandling.id).isEqualTo(fagsak.id)
        assertThat(fagsakForBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun findByEksternIdAndFagsystemAndStønadstype() {
        val eksternId = Random.nextInt().toString()
        val fagsystem = Fagsystem.TILLEGGSSTONADER
        val stønadstype = Stønadstype.BARNETILSYN

        val lagretFagsak =
            testoppsettService.lagreFagsak(
                fagsakDomain(
                    eksternId = eksternId,
                    fagsystem = fagsystem,
                    stønadstype = stønadstype,
                ).tilFagsakMedPerson(setOf(PersonIdent("1"))),
            )

        val fagsak =
            fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(
                eksternId = eksternId,
                fagsystem = fagsystem,
                stønadstype = stønadstype,
            )!!

        assertThat(lagretFagsak.id).isEqualTo(fagsak.id)
        assertThat(lagretFagsak.eksternId).isEqualTo(fagsak.eksternId)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
    }
}
