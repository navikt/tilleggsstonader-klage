package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.Opprettet
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.HenlagtÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
    val behandlingId = BehandlingId.random()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    fun insertBehandling() {
        val påklagetVedtakDetaljer =
            PåklagetVedtakDetaljer(
                fagsystemType = FagsystemType.ORDNIÆR,
                eksternFagsystemBehandlingId = "1234",
                behandlingstype = "type",
                resultat = "resultat",
                vedtakstidspunkt = LocalDateTime.now(),
                regelverk = Regelverk.NASJONAL,
            )
        val fagsystemRevurdering = FagsystemRevurdering(true, Opprettet("id", LocalDateTime.now()), null)
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    id = behandlingId,
                    klageMottatt = LocalDate.now(),
                    påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    henlagtÅrsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    henlagtBegrunnelse = "Skal trekkes tilbake",
                    fagsystemRevurdering = fagsystemRevurdering,
                ),
            )

        val hentetBehandling = behandlingRepository.findByIdOrThrow(behandlingId)

        assertThat(behandling.id).isEqualTo(hentetBehandling.id)
        assertThat(behandling.fagsakId).isEqualTo(hentetBehandling.fagsakId)
        assertThat(behandling.eksternBehandlingId).isEqualTo(hentetBehandling.eksternBehandlingId)
        assertThat(behandling.påklagetVedtak).isEqualTo(hentetBehandling.påklagetVedtak)
        assertThat(behandling.klageMottatt).isEqualTo(hentetBehandling.klageMottatt)
        assertThat(behandling.resultat).isEqualTo(hentetBehandling.resultat)
        assertThat(behandling.henlagtÅrsak).isEqualTo(HenlagtÅrsak.TRUKKET_TILBAKE)
        assertThat(behandling.henlagtBegrunnelse).isEqualTo(hentetBehandling.henlagtBegrunnelse)
        assertThat(behandling.sporbar.opprettetAv).isEqualTo(hentetBehandling.sporbar.opprettetAv)
        assertThat(behandling.sporbar.opprettetTid).isEqualTo(hentetBehandling.sporbar.opprettetTid)
        assertThat(behandling.sporbar.endret.endretTid).isEqualTo(hentetBehandling.sporbar.endret.endretTid)
        assertThat(behandling.sporbar.endret.endretAv).isEqualTo(hentetBehandling.sporbar.endret.endretAv)
        assertThat(behandling.fagsystemRevurdering).isEqualTo(hentetBehandling.fagsystemRevurdering)
    }

    @Test
    fun updateStatus() {
        val behandling = behandlingRepository.insert(behandling(fagsak, behandlingId))

        assertThat(behandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        val nyStatus = BehandlingStatus.UTREDES

        behandlingRepository.updateStatus(behandlingId, nyStatus)

        assertThat(behandlingRepository.findByIdOrThrow(behandlingId).status).isEqualTo(nyStatus)
    }

    @Test
    fun updateSteg() {
        val behandling = behandlingRepository.insert(behandling(fagsak, behandlingId))

        assertThat(behandling.steg).isEqualTo(StegType.FORMKRAV)

        val nyttSteg = StegType.VURDERING
        behandlingRepository.updateSteg(behandlingId, nyttSteg)

        assertThat(behandlingRepository.findByIdOrThrow(behandlingId).steg).isEqualTo(nyttSteg)
    }

    @Test
    internal fun `findByEksternBehandlingIdAndFagsystem - forvent treff`() {
        val fagsakPersistert =
            testoppsettService.lagreFagsak(
                fagsakDomain().tilFagsakMedPerson(
                    setOf(PersonIdent("12345678901")),
                ),
            )

        val fagsakPersistert2 =
            testoppsettService.lagreFagsak(
                fagsakDomain().tilFagsakMedPerson(
                    setOf(PersonIdent("12345678902")),
                ),
            )

        val behandlingPersistert = behandlingRepository.insert(behandling(fagsakPersistert))
        behandlingRepository.insert(behandling(fagsakPersistert2))

        val behandling = behandlingRepository.findByEksternBehandlingId(behandlingPersistert.eksternBehandlingId)
        assertThat(behandling).isNotNull
        assertThat(behandling.id).isEqualTo(behandlingPersistert.id)
        assertThat(fagsakPersistert.id).isEqualTo(behandling.fagsakId)
    }

    @Nested
    inner class FinnKlagebehandlingsresultat {
        @Test
        internal fun `skal returnere tom liste når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.TILLEGGSSTONADER))
                .isEmpty()
        }

        @Test
        internal fun `skal returnere tom liste når det kun finnes behandlinger på en annen fagsak`() {
            val fagsak2 = testoppsettService.lagreFagsak(fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("2"))))
            behandlingRepository.insert(behandling(fagsak2))

            assertThat(behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.TILLEGGSSTONADER))
                .isEmpty()
        }

        @Test
        internal fun `skal finne alle behandlinger for eksternFagsakId`() {
            val behandling = behandlingRepository.insert(behandling(fagsak))
            val behandling2 = behandlingRepository.insert(behandling(fagsak))

            val behandlinger =
                behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.TILLEGGSSTONADER)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        @Test
        internal fun `skal mappe verdier fra repository til klageBehandling`() {
            val behandling = behandlingRepository.insert(behandling(fagsak))
            val behandling2 = behandlingRepository.insert(behandling(fagsak))

            val behandlinger =
                behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.TILLEGGSSTONADER)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        @Test
        fun `skal finne en treff med siste identen hvis det finnes flere identer på en fagsak`() {
            val behandling = behandlingRepository.insert(behandling(fagsak))
            val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
            jdbcTemplate.update("DELETE FROM person_ident", emptyMap<String, String>())
            leggInnPersonIdent(fagsak, "1", LocalDateTime.now().minusDays(2))
            leggInnPersonIdent(fagsak, "2", LocalDateTime.now())
            leggInnPersonIdent(fagsak, "3", LocalDateTime.now().minusDays(1))

            val resultat = behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, fagsak.fagsystem)
            assertThat(resultat).hasSize(1)
            assertThat(resultat.single().ident).isEqualTo("2")
        }

        private fun leggInnPersonIdent(
            fagsak: FagsakDomain,
            ident: String,
            tidspunkt: LocalDateTime,
        ) {
            jdbcTemplate.update(
                "INSERT INTO person_ident (fagsak_person_id, ident, opprettet_tid, opprettet_av, endret_tid, endret_av) " +
                    "VALUES (:fagsakPersonId, :ident, :opprettetTid, :opprettetAv, :endretTid, :endretAv)",
                mapOf(
                    "fagsakPersonId" to fagsak.fagsakPersonId,
                    "ident" to ident,
                    "opprettetTid" to tidspunkt,
                    "opprettetAv" to "VL",
                    "endretTid" to tidspunkt,
                    "endretAv" to "VL",
                ),
            )
        }
    }
}
