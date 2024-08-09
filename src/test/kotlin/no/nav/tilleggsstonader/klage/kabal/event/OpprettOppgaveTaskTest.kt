package no.nav.tilleggsstonader.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.fagsak.FagsakPersonRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.oppgave.OppgaveService
import no.nav.tilleggsstonader.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.tilleggsstonader.klage.oppgave.OpprettOppgavePayload
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class OpprettOppgaveTaskTest : IntegrationTest() {

    @Autowired lateinit var fagsakRepository: FagsakRepository

    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @Autowired lateinit var personRepository: FagsakPersonRepository

    private val oppgaveService = mockk<OppgaveService>()
    private val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

    private lateinit var opprettOppgaveTask: OpprettKabalEventOppgaveTask

    val personIdent = "12345678901"
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        opprettOppgaveTask = OpprettKabalEventOppgaveTask(fagsakRepository, behandlingRepository, personRepository, oppgaveService)
        every { oppgaveService.opprettOppgave(capture(opprettOppgaveRequestSlot)) } answers { 9L }

        fagsak = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent(personIdent),
                ),
            ),
        )
        behandling = behandling(fagsak)

        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal lage oppgave med riktige verdier i request`() {
        val fagsakDomain = fagsakRepository.findByIdOrNull(fagsak.id) ?: error("Finner ikke fagsak med id")

        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.TILLEGGSSTONADER, null, behandlingstema = Behandlingstema.TilsynBarn)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload))

        assertThat(opprettOppgaveRequestSlot.captured.tema).isEqualTo(Tema.TSO)
        assertThat(opprettOppgaveRequestSlot.captured.beskrivelse).contains("tekst")
        assertThat(opprettOppgaveRequestSlot.captured.ident).isEqualTo(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(opprettOppgaveRequestSlot.captured.saksreferanse).isEqualTo(fagsakDomain.eksternId)
        assertThat(opprettOppgaveRequestSlot.captured.enhetsnummer).isEqualTo(behandling.behandlendeEnhet)
        assertThat(opprettOppgaveRequestSlot.captured.behandlingstema).isEqualTo(Behandlingstema.TilsynBarn.value)
        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.NORM)
    }

    @Test
    fun `skal gi høy prioritet til oppgaver med klageinnstansutfall lik opphevet`() {
        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.TILLEGGSSTONADER, KlageinstansUtfall.OPPHEVET)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload))

        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.HOY)
    }

    @Test
    fun `skal gi norm prioritet til oppgaver med klageinnstansutfall ikke lik opphevet`() {
        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.TILLEGGSSTONADER, KlageinstansUtfall.MEDHOLD)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload))

        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.NORM)
    }
}
