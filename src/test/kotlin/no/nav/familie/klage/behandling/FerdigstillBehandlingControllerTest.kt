package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.distribusjon.DistribuerBrevTask
import no.nav.tilleggsstonader.klage.distribusjon.JournalførBrevTask
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.formkrav.dto.tilDto
import no.nav.tilleggsstonader.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.tilleggsstonader.klage.infrastruktur.config.RolleConfig
import no.nav.tilleggsstonader.klage.oppgave.BehandleSakOppgave
import no.nav.tilleggsstonader.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.oppfyltForm
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurderingDto
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class FerdigstillBehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandleSakOppgaveRepository: BehandleSakOppgaveRepository

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    val fagsak = fagsakDomain().tilFagsak()
    val behandling = behandling(fagsak = fagsak)
    val vurdering = vurderingDto(behandlingId = behandling.id)

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        BrukerContextUtil.mockBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler))

        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        formService.opprettInitielleFormkrav(behandling.id)
        formService.oppdaterFormkrav(oppfyltForm(behandling.id).tilDto(PåklagetVedtakDto("123", VEDTAK)))
        vurderingService.opprettEllerOppdaterVurdering(vurdering)

        brevService.lagBrev(behandling.id)
        behandleSakOppgaveRepository.insert(
            BehandleSakOppgave(
                behandlingId = behandling.id,
                oppgaveId = Random.nextLong().absoluteValue,
            ),
        )
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling og opprette tasks for distribuering av data til dokarkiv og kabal`() {
        val ferdigstillResponse = ferdigstill(behandlingId = behandling.id)
        assertThat(ferdigstillResponse.statusCode).isEqualTo(HttpStatus.OK)
        val journalførTask = taskService.finnTaskMedPayloadOgType(behandling.id.toString(), JournalførBrevTask.TYPE)
        assertThat(journalførTask).isNotNull
        println(journalførTask)
        if (journalførTask?.status == Status.FERDIG) {
            println("STATUS ER FERDIG - sjekker for distribuertask")
            assertThat(taskService.finnTaskMedPayloadOgType(behandling.id.toString(), DistribuerBrevTask.TYPE)).isNotNull
        }
    }

    private fun ferdigstill(behandlingId: UUID): ResponseEntity<Ressurs<Unit>> {
        return restTemplate.exchange(
            localhost("/api/behandling/$behandlingId/ferdigstill"),
            HttpMethod.POST,
            HttpEntity(null, headers),
        )
    }
}
