package no.nav.tilleggsstonader.klage.ekstern

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.felles.domain.SporbarUtils
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.klageresultat
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.VurderingRepository
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.HenlagtÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.expectBody

internal class EksternBehandlingControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vurderingRepository: VurderingRepository

    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    private val fagsak =
        DomainUtil
            .fagsakDomain(eksternId = "1", stønadstype = Stønadstype.BARNETILSYN)
            .tilFagsakMedPerson(setOf(PersonIdent("1")))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Nested
    inner class FinnKlagebehandlingsresultat {
        private val hentBehandlingUrl: String = localhost("/api/ekstern/behandling/${Fagsystem.TILLEGGSSTONADER}")

        @Test
        internal fun `skal returnere tomt svar når det ikke finnes noen behandlinger på fagsaken`() {
            val externFagsakId = "200"
            val url = "$hentBehandlingUrl?eksternFagsakId=$externFagsakId"
            val behandlinger = hentBehandlinger(url)

            assertThat(behandlinger).isEqualTo(mapOf(externFagsakId to emptyList<KlagebehandlingDto>()))
        }

        @Test
        internal fun `skal returnere behandling når man spør etter eksternFagsakId`() {
            val vedtakDato = SporbarUtils.now()
            val henlagtÅrsak = HenlagtÅrsak.TRUKKET_TILBAKE
            val behandling =
                behandlingRepository.insert(
                    behandling(fagsak, vedtakDato = vedtakDato, henlagtÅrsak = henlagtÅrsak),
                )
            vurderingRepository.insert(vurdering(behandling.id, årsak = Årsak.FEIL_PROSESSUELL))
            val klageresultat = klageresultatRepository.insert(klageresultat(behandlingId = behandling.id))

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId}"
            val behandlinger = hentBehandlinger(url)

            assertThat(behandlinger).hasSize(1)
            val behandlingerPerFagsak = behandlinger.entries.single()
            assertThat(behandlingerPerFagsak.key).isEqualTo(fagsak.eksternId)
            assertThat(behandlingerPerFagsak.value).hasSize(1)

            with(behandlingerPerFagsak.value.single()) {
                assertThat(id).isEqualTo(behandling.id.id)
                assertThat(fagsakId).isEqualTo(behandling.fagsakId)
                assertThat(status).isEqualTo(behandling.status)
                assertThat(mottattDato).isEqualTo(behandling.klageMottatt)
                assertThat(opprettet).isEqualTo(behandling.sporbar.opprettetTid)
                assertThat(resultat).isEqualTo(BehandlingResultat.IKKE_SATT)
                assertThat(årsak).isEqualTo(Årsak.FEIL_PROSESSUELL)
                assertThat(vedtaksdato).isEqualTo(vedtakDato)
                assertThat(henlagtÅrsak).isEqualTo(henlagtÅrsak)
            }

            val klageinstansResultat = behandlingerPerFagsak.value.single().klageinstansResultat
            with(klageinstansResultat.single()) {
                assertThat(type).isEqualTo(klageresultat.type)
                assertThat(utfall).isEqualTo(klageresultat.utfall)
                assertThat(mottattEllerAvsluttetTidspunkt)
                    .isEqualTo(klageresultat.mottattEllerAvsluttetTidspunkt)
                assertThat(journalpostReferanser)
                    .containsExactlyInAnyOrderElementsOf(klageresultat.journalpostReferanser.verdier)
            }
        }

        private fun hentBehandlinger(url: String): Map<String, List<KlagebehandlingDto>> =
            restTestClient
                .get()
                .uri(url)
                .headers { it.addAll(headers) }
                .exchangeSuccessfully()
                .expectBody<Map<String, List<KlagebehandlingDto>>>()
                .returnResult()
                .responseBody!!
    }
}
