package no.nav.tilleggsstonader.klage.ekstern

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.Ressurs
import no.nav.tilleggsstonader.klage.Ressurs.Status
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.exchange

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
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            assertThat(body.data).isEqualTo(mapOf(externFagsakId to emptyList<KlagebehandlingDto>()))
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
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            val data = body.data
            assertThat(data!!).hasSize(1)
            val entry = data.entries.single()
            assertThat(entry.key).isEqualTo(fagsak.eksternId)
            assertThat(entry.value).hasSize(1)

            val klagebehandling = entry.value.single()
            assertThat(klagebehandling.id).isEqualTo(behandling.id.id)
            assertThat(klagebehandling.fagsakId).isEqualTo(behandling.fagsakId)
            assertThat(klagebehandling.status).isEqualTo(behandling.status)
            assertThat(klagebehandling.mottattDato).isEqualTo(behandling.klageMottatt)
            assertThat(klagebehandling.opprettet).isEqualTo(behandling.sporbar.opprettetTid)
            assertThat(klagebehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)
            assertThat(klagebehandling.årsak).isEqualTo(Årsak.FEIL_PROSESSUELL)
            assertThat(klagebehandling.vedtaksdato).isEqualTo(vedtakDato)
            assertThat(klagebehandling.henlagtÅrsak).isEqualTo(henlagtÅrsak)

            val klageinstansResultat = klagebehandling.klageinstansResultat
            assertThat(klageinstansResultat).hasSize(1)
            assertThat(klageinstansResultat[0].type).isEqualTo(klageresultat.type)
            assertThat(klageinstansResultat[0].utfall).isEqualTo(klageresultat.utfall)
            assertThat(klageinstansResultat[0].mottattEllerAvsluttetTidspunkt)
                .isEqualTo(klageresultat.mottattEllerAvsluttetTidspunkt)
            assertThat(klageinstansResultat[0].journalpostReferanser)
                .containsExactlyInAnyOrderElementsOf(klageresultat.journalpostReferanser.verdier)
        }

        private fun hentBehandlinger(url: String) =
            restTemplate.exchange<Ressurs<Map<String, List<KlagebehandlingDto>>>>(url, HttpMethod.GET, HttpEntity(null, headers))
    }
}
