package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.domain.Avsnitt
import no.nav.tilleggsstonader.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.tilleggsstonader.klage.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AvsnittRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var avsnittRepository: AvsnittRepository

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)
    private val behandling2 = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        testoppsettService.lagreBehandling(behandling2)
    }

    @Test
    internal fun `henting og lagring`() {
        val avsnitt = Avsnitt(behandlingId = behandling.id, deloverskrift = "deloverskrift", innhold = "innhold")
        avsnittRepository.insert(avsnitt)

        val oppdatertAvsnitt = avsnittRepository.findByIdOrThrow(avsnitt.avsnittId)

        assertThat(oppdatertAvsnitt.avsnittId).isEqualTo(avsnitt.avsnittId)
        assertThat(oppdatertAvsnitt.behandlingId).isEqualTo(avsnitt.behandlingId)
        assertThat(oppdatertAvsnitt.deloverskrift).isEqualTo(avsnitt.deloverskrift)
        assertThat(oppdatertAvsnitt.innhold).isEqualTo(avsnitt.innhold)
        assertThat(oppdatertAvsnitt.skalSkjulesIBrevbygger).isEqualTo(avsnitt.skalSkjulesIBrevbygger)
    }

    @Test
    internal fun `findByBehandlingId skal kun finne avsnitt hvis det finnes på den behandlingen`() {
        avsnittRepository.insert(Avsnitt(behandlingId = behandling.id, deloverskrift = "", innhold = ""))
        avsnittRepository.insert(Avsnitt(behandlingId = behandling.id, deloverskrift = "", innhold = ""))

        assertThat(avsnittRepository.findByBehandlingId(behandling2.id)).isEmpty()
        assertThat(avsnittRepository.findByBehandlingId(behandling.id)).hasSize(2)
    }

    @Test
    internal fun `skal slette avsnitt for gitt behandling`() {
        avsnittRepository.insert(Avsnitt(behandlingId = behandling.id, deloverskrift = "", innhold = ""))
        avsnittRepository.slettAvsnittMedBehandlingId(behandling2.id)
        assertThat(avsnittRepository.findByBehandlingId(behandling.id)).hasSize(1)

        avsnittRepository.slettAvsnittMedBehandlingId(behandling.id)
        assertThat(avsnittRepository.findByBehandlingId(behandling.id)).isEmpty()
    }
}
