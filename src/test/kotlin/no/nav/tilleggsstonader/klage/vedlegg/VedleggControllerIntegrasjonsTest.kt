package no.nav.tilleggsstonader.klage.vedlegg

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.exchange

internal class VedleggControllerIntegrasjonsTest : IntegrationTest() {
    final val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak)

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    internal fun `skal hente ut all metadata om dokumenter samt ett vedlegg`() {
        val vedleggMetadataResponse = finnVedlegg(behandling.id)
        assertThat(vedleggMetadataResponse.statusCode).isEqualTo(HttpStatus.OK)
        val førsteDokumentMetadata = vedleggMetadataResponse.body?.first()
        assertThat(førsteDokumentMetadata).isNotNull
        førsteDokumentMetadata ?: error("Mangler metadata til dokument")
    }

    private fun finnVedlegg(behandlingId: BehandlingId): ResponseEntity<List<DokumentinfoDto>> =
        restTemplate.exchange(
            localhost("/api/vedlegg/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
