package no.nav.tilleggsstonader.klage.kabal

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.DatabaseConfiguration
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class BehandlingFeilregistrertTaskTest : IntegrationTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @Autowired lateinit var stegService: StegService

    @Autowired lateinit var taskService: TaskService

    @Autowired lateinit var behandlingService: BehandlingService

    @Autowired lateinit var fagsakService: FagsakService

    @Autowired lateinit var klageresultatRepository: KlageresultatRepository

    private lateinit var behandlingFeilregistrertTask: BehandlingFeilregistrertTask

    val personIdent = "12345678901"
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        behandlingFeilregistrertTask =
            BehandlingFeilregistrertTask(stegService, taskService, behandlingService, fagsakService)

        fagsak = testoppsettService.lagreFagsak(
            DomainUtil.fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent(personIdent),
                ),
            ),
        )
        behandling = DomainUtil.behandling(
            fagsak = fagsak,
            resultat = BehandlingResultat.IKKE_MEDHOLD,
            status = BehandlingStatus.VENTER,
            steg = StegType.KABAL_VENTER_SVAR,
        )

        behandlingRepository.insert(behandling)

        klageresultatRepository.insert(
            KlageinstansResultat(
                eventId = UUID.randomUUID(),
                type = BehandlingEventType.BEHANDLING_FEILREGISTRERT,
                utfall = null,
                mottattEllerAvsluttetTidspunkt = LocalDateTime.of(2023, 6, 22, 1, 1),
                kildereferanse = behandling.eksternBehandlingId,
                journalpostReferanser = DatabaseConfiguration.StringListWrapper(verdier = listOf()),
                behandlingId = behandling.id,
                årsakFeilregistrert = "fordi det var feil",
            ),
        )
    }

    @Test
    internal fun `task skal opprette OpprettOppgaveTask og ferdigstille behandling`() {
        assertThat(behandling.steg).isEqualTo(StegType.KABAL_VENTER_SVAR)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.VENTER)

        behandlingFeilregistrertTask.doTask(BehandlingFeilregistrertTask.opprettTask(behandling.id))

        val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
        assertThat(oppdatertBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)

        val opprettOppgaveTask = taskService.findAll().single { it.type == OpprettKabalEventOppgaveTask.TYPE }
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(opprettOppgaveTask.payload)
        assertThat(opprettOppgavePayload.oppgaveTekst).isEqualTo("Klagebehandlingen er sendt tilbake fra KA med status feilregistrert.\n\nBegrunnelse fra KA: \"fordi det var feil\"")

        assertThat(opprettOppgavePayload.klagebehandlingEksternId).isEqualTo(behandling.eksternBehandlingId)
        assertThat(opprettOppgavePayload.fagsystem).isEqualTo(fagsak.fagsystem)
        assertThat(opprettOppgavePayload.behandlingstema).isNull()
        assertThat(opprettOppgavePayload.behandlingstype).isEqualTo(Behandlingstema.Klage.value)
    }
}
