package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandleSakOppgaveRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandleSakOppgaveRepository: BehandleSakOppgaveRepository

    val fagsak1 = fagsak()
    val fagsak2 = fagsak(identer = setOf(PersonIdent("2")))
    val behandling1 = behandling(fagsak1)
    val behandling2 = behandling(fagsak2)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak1)
        testoppsettService.lagreFagsak(fagsak2)
        testoppsettService.lagreBehandling(behandling1)
        testoppsettService.lagreBehandling(behandling2)
    }

    @Test
    fun `skal kunne hente oppgaver på oppgaveIder`() {
        val behandleSakOppgave1 =
            behandleSakOppgaveRepository.insert(BehandleSakOppgave(behandlingId = behandling1.id, oppgaveId = 1))
        val behandleSakOppgave2 =
            behandleSakOppgaveRepository.insert(BehandleSakOppgave(behandlingId = behandling2.id, oppgaveId = 2))

        assertThat(behandleSakOppgaveRepository.finnForOppgaveIder(listOf(1)))
            .containsExactly(behandleSakOppgave1)
        assertThat(behandleSakOppgaveRepository.finnForOppgaveIder(listOf(1, 2)))
            .containsExactlyInAnyOrder(behandleSakOppgave1, behandleSakOppgave2)
    }
}
