package no.nav.tilleggsstonader.klage.vedlegg

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.client.expectBody

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
        val førsteDokumentMetadata = vedleggMetadataResponse.first()
        assertThat(førsteDokumentMetadata).isNotNull
    }

    private fun finnVedlegg(behandlingId: BehandlingId): List<DokumentinfoDto> =
        restTestClient
            .get()
            .uri(
                localhost("/api/vedlegg/$behandlingId"),
            ).headers { it.addAll(headers) }
            .exchangeSuccessfully()
            .expectBody<List<DokumentinfoDto>>()
            .returnResult()
            .responseBody!!
}
