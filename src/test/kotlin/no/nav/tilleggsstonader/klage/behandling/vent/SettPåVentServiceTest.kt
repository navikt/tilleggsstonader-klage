package no.nav.tilleggsstonader.klage.behandling.vent

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.klage.behandlingshistorikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.klage.behandlingshistorikk.dto.Hendelse
import no.nav.tilleggsstonader.klage.behandlingshistorikk.dto.tilDto
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_KLAR
import no.nav.tilleggsstonader.klage.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_PÅ_VENT
import no.nav.tilleggsstonader.klage.infrastruktur.mocks.OppgaveTestClient
import no.nav.tilleggsstonader.klage.oppgave.OppgaveService
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class SettPåVentServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var settPåVentService: SettPåVentService

    @Autowired
    lateinit var settPåVentRepository: SettPåVentRepository

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var oppgaveTestClient: OppgaveTestClient

    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    val behandling = behandling()
    var oppgaveId: Long? = null

    val settPåVentDto =
        SettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.ANNET),
            frist = LocalDate.now().plusDays(3),
            kommentar = "ny beskrivelse",
        )

    val oppdaterSettPåVentDto =
        OppdaterSettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.REGISTRERING_AV_TILTAK),
            frist = LocalDate.now().plusDays(5),
            kommentar = "oppdatert beskrivelse",
            oppgaveVersjon = 1,
        )

    val dummySaksbehandler = "saksbehandler1"

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreBehandlingMedFagsak(behandling)
        oppgaveId =
            oppgaveService.opprettOppgave(
                behandling.id,
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = "123456789012", gruppe = IdentGruppe.AKTOERID),
                    fristFerdigstillelse = LocalDate.now().plusDays(3),
                    behandlingstema = "behandlingstema",
                    enhetsnummer = "enhetsnummer",
                    tema = Tema.TSO,
                    oppgavetype = Oppgavetype.BehandleSak,
                    mappeId = 1234L,
                    beskrivelse = "Oppgavetekst",
                    tilordnetRessurs = dummySaksbehandler,
                ),
            )
    }

    @Nested
    inner class SettPåVent {
        @Test
        fun `skal sette behandling på vent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)

                assertThat(testoppsettService.hentBehandling(behandling.id).status)
                    .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

                with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                    assertThat(årsaker).isEqualTo(settPåVentDto.årsaker)
                    assertThat(frist).isEqualTo(settPåVentDto.frist)
                    assertThat(kommentar).contains("ny beskrivelse")
                }

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(beskrivelse).contains("ny beskrivelse")
                    assertThat(fristFerdigstillelse).isEqualTo(settPåVentDto.frist)
                    assertThat(tilordnetRessurs).isNull()
                    assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT)
                }

                with(behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).maxByOrNull { it.endretTid }!!) {
                    assertThat(utfall).isEqualTo(StegUtfall.SATT_PÅ_VENT)
                    assertThat(metadata).isNotNull()
                }
            }
        }

        @Test
        fun `skal sette behandling på vent og fortsette beholde oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(tilordnetRessurs).isEqualTo(dummySaksbehandler)
                }
            }
        }

        @Test
        fun `skal feile hvis man ikke er eier av oppgaven`() {
            testWithBrukerContext {
                assertThatThrownBy {
                    settPåVentService.settPåVent(behandling.id, settPåVentDto)
                }.hasMessageContaining("Kan ikke sette behandling på vent når man ikke er eier av oppgaven.")
            }
        }

        @Test
        fun `skal feile hvis man prøver å sette behandling på vent når den allerede er på vent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }
            assertThatThrownBy {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }.hasMessageContaining("Kan ikke sette behandling på vent når behandling har status=SATT_PÅ_VENT")
        }
    }

    @Nested
    inner class OppdaterSettPåVent {
        @Test
        fun `skal kunne oppdatere settPåVent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                settPåVentService.oppdaterSettPåVent(behandling.id, oppdaterSettPåVentDto.copy(oppgaveVersjon = 3))

                assertThat(testoppsettService.hentBehandling(behandling.id).status)
                    .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

                with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                    assertThat(årsaker).isEqualTo(oppdaterSettPåVentDto.årsaker)
                    assertThat(frist).isEqualTo(oppdaterSettPåVentDto.frist)
                    assertThat(kommentar).contains("oppdatert beskrivelse")
                }

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(beskrivelse).contains("oppdatert beskrivelse")
                    assertThat(fristFerdigstillelse).isEqualTo(oppdaterSettPåVentDto.frist)
                    assertThat(tilordnetRessurs).isNull()
                    assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT)
                }
            }
        }

        @Test
        fun `skal sette behandling på vent og fortsette beholde oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                val dto = oppdaterSettPåVentDto.copy(oppgaveVersjon = 3, beholdOppgave = true)
                settPåVentService.oppdaterSettPåVent(behandling.id, dto)

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(tilordnetRessurs).isEqualTo(dummySaksbehandler)
                }
            }
        }

        @Test
        fun `skal feile hvis man ikke er eier av oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }
            testWithBrukerContext {
                assertThatThrownBy {
                    settPåVentService.oppdaterSettPåVent(behandling.id, oppdaterSettPåVentDto.copy(oppgaveVersjon = 3))
                }.hasMessageContaining("Kan ikke oppdatere behandling på vent når man ikke er eier av oppgaven.")
            }
        }
    }

    @Nested
    inner class TaAvVent {
        @BeforeEach
        fun setUp() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))
            }
        }

        @Test
        fun `skal ta av vent og fortsette behandling - uten dto`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, null)
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og fortsette behandling - uten kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = true, kommentar = null))
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og fortsette behandling - med kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(
                    behandling.id,
                    TaAvVentDto(skalTilordnesRessurs = true, kommentar = "årsak av vent"),
                )
            }

            validerTattAvVent(behandling.id, kommentar = "årsak av vent")
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
        }

        @Test
        fun `skal ta av vent og markere oppgave som ufordelt - uten kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = false, kommentar = null))
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og markere oppgave som ufordelt - med kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(
                    behandling.id,
                    TaAvVentDto(skalTilordnesRessurs = false, kommentar = "årsak av vent"),
                )
            }

            validerTattAvVent(behandling.id, kommentar = "årsak av vent")
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
        }

        @Test
        fun `skal feile hvis man ikke er eier av oppgaven`() {
            testWithBrukerContext {
                assertThatThrownBy {
                    settPåVentService.taAvVent(
                        behandling.id,
                        TaAvVentDto(skalTilordnesRessurs = false, kommentar = "årsak av vent"),
                    )
                }.hasMessageContaining("Kan ikke ta behandling av vent når man ikke er eier av oppgaven.")
            }
        }

        private fun validerTattAvVent(
            behandlingId: BehandlingId,
            kommentar: String? = null,
        ) {
            with(settPåVentRepository.findAll().single()) {
                assertThat(aktiv).isFalse()
                assertThat(taAvVentKommentar).isEqualTo(kommentar)
            }

            assertThat(testoppsettService.hentBehandling(behandlingId).status)
                .isEqualTo(BehandlingStatus.UTREDES)
        }

        private fun validerOppdatertOppgave(
            oppgaveId: Long,
            tilordnetRessurs: String?,
        ) {
            with(oppgaveService.hentOppgave(oppgaveId)) {
                assertThat(tilordnetRessurs).isEqualTo(tilordnetRessurs)
                assertThat(beskrivelse).contains("Tatt av vent")
                assertThat(fristFerdigstillelse).isEqualTo(LocalDate.now())
                assertThat(mappeId).isEqualTo(Optional.of(MAPPE_ID_KLAR))
            }
        }

        private fun validerHistorikkInnslag(
            behandlingId: BehandlingId,
            skalHaMetadata: Boolean,
        ) {
            with(behandlingshistorikkService.hentBehandlingshistorikk(behandlingId).maxByOrNull { it.endretTid }!!) {
                assertThat(utfall).isEqualTo(StegUtfall.TATT_AV_VENT)
                if (skalHaMetadata) {
                    assertThat(metadata).isNotNull()
                } else {
                    assertThat(metadata).isNull()
                }
            }
        }
    }

    @Nested
    inner class KanTaAvVent {
        @BeforeEach
        fun setUp() {
            testWithBrukerContext(preferredUsername = dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }
        }

        @Test
        fun `retunerer OK når behandlingen kan tas av vent`() {
            val res =
                testWithBrukerContext {
                    settPåVentService.kanTaAvVent(behandling.id)
                }
            assertThat(res).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.OK))
        }

        @Test
        fun `retunerer ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAGKEN når det er annen aktiv behandling på fagsaken`() {
            val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
            val aktivBehandling =
                behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            testoppsettService.lagreBehandling(aktivBehandling)

            val res = settPåVentService.kanTaAvVent(behandling.id)
            assertThat(res).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAKEN))
        }
    }

    @Nested
    inner class Historikk {
        @Test
        fun `skal returnere kommentar fra historikk når behandlingen ikke ennå er sendt til iverksetting eller ferdigstilt`() {
            val taAvVentDto = TaAvVentDto(skalTilordnesRessurs = false, kommentar = "tatt av")

            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                settPåVentService.taAvVent(behandling.id, taAvVentDto)
            }

            val historikk = behandlingshistorikkService.hentBehandlingshistorikk(behandling.id)
            historikk.finnMetadata(Hendelse.SATT_PÅ_VENT).assertMetadataInneholderEksakt(
                mapOf(
                    "kommentarSettPåVent" to "ny beskrivelse",
                    "årsaker" to listOf(ÅrsakSettPåVent.ANNET.name),
                ),
            )
            historikk.finnMetadata(Hendelse.TATT_AV_VENT).assertMetadataInneholderEksakt(
                mapOf(
                    "kommentar" to "tatt av",
                ),
            )
        }

        private fun List<Behandlingshistorikk>.finnMetadata(hendelse: Hendelse) = this.tilDto().single { it.hendelse == hendelse }

        private fun BehandlingshistorikkDto.assertMetadataInneholderEksakt(map: Map<String, Any>) {
            assertThat(this.metadata).containsExactlyInAnyOrderEntriesOf(map)
        }
    }

    private fun plukkOppgaven() {
        oppgaveTestClient.plukkOppgave(oppgaveId!!)
    }
}
