package no.nav.tilleggsstonader.klage.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.journalpost.JournalpostService
import no.nav.tilleggsstonader.klage.testutil.DomainUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.tilFagsak
import no.nav.tilleggsstonader.kontrakter.journalpost.RelevantDato
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class VedleggServiceTest {
    val behandlingServiceMock = mockk<BehandlingService>()
    val journalpostServiceMock = mockk<JournalpostService>()
    val vedleggService = VedleggService(behandlingServiceMock, journalpostServiceMock)

    val fagsak = DomainUtil.fagsakDomain().tilFagsak()

    val dokument = DomainUtil.journalpostDokument()

    val datoRegistrert = RelevantDato(LocalDateTime.now().minusDays(3), "DATO_REGISTRERT")
    val datoJournalført = RelevantDato(LocalDateTime.now().minusDays(4), "DATO_JOURNALFOERT")
    val datoDokument = RelevantDato(LocalDateTime.now().minusDays(5), "DATO_DOKUMENT")
    val datoOpprettet = RelevantDato(LocalDateTime.now().minusDays(2), "DATO_OPPRETTET")
    val datoUkjent = RelevantDato(LocalDateTime.now().minusDays(1), "DATO_UKJENT")

    @BeforeEach
    internal fun setUp() {
        every { behandlingServiceMock.hentAktivIdent(any()) } returns Pair("12345678910", fagsak)
    }

    @Test
    internal fun `skal sette datoRegistrert på journalpost dersom finnes`() {
        val journalPost =
            DomainUtil.journalpost(
                listOf(dokument),
                listOf(datoUkjent, datoDokument, datoJournalført, datoRegistrert, datoOpprettet),
            )

        every { journalpostServiceMock.finnJournalposter(any(), any()) } returns listOf(journalPost)

        val vedlegg = vedleggService.finnVedleggPåBehandling(BehandlingId.random())

        Assertions.assertThat(vedlegg.first().dato).isEqualToIgnoringNanos(datoRegistrert.dato)
    }

    @Test
    internal fun `skal sette datoJournalført dersom datoRegistrert ikke finnes og datoJournalført finnes`() {
        val journalPost =
            DomainUtil.journalpost(
                listOf(dokument),
                listOf(datoUkjent, datoDokument, datoJournalført, datoOpprettet),
            )

        every { journalpostServiceMock.finnJournalposter(any(), any()) } returns listOf(journalPost)

        val vedlegg = vedleggService.finnVedleggPåBehandling(BehandlingId.random())

        Assertions.assertThat(vedlegg.first().dato).isEqualToIgnoringNanos(datoJournalført.dato)
    }

    @Test
    internal fun `skal sette datoDokument forran datoOpprettet`() {
        val journalPost =
            DomainUtil.journalpost(
                listOf(dokument),
                listOf(datoUkjent, datoDokument, datoOpprettet),
            )

        every { journalpostServiceMock.finnJournalposter(any(), any()) } returns listOf(journalPost)

        val vedlegg = vedleggService.finnVedleggPåBehandling(BehandlingId.random())

        Assertions.assertThat(vedlegg.first().dato).isEqualToIgnoringNanos(datoDokument.dato)
    }

    @Test
    internal fun `skal sette datoOpprettet dersom ingen annen dato finnes og datoOpprettet finnes`() {
        val journalPost =
            DomainUtil.journalpost(
                listOf(dokument),
                listOf(datoUkjent, datoOpprettet),
            )

        every { journalpostServiceMock.finnJournalposter(any(), any()) } returns listOf(journalPost)

        val vedlegg = vedleggService.finnVedleggPåBehandling(BehandlingId.random())

        Assertions.assertThat(vedlegg.first().dato).isEqualToIgnoringNanos(datoOpprettet.dato)
    }

    @Test
    internal fun `skal ikke sette dato dersom dato ikke finnes`() {
        val journalPost =
            DomainUtil.journalpost(
                listOf(dokument),
                emptyList(),
            )

        every { journalpostServiceMock.finnJournalposter(any(), any()) } returns listOf(journalPost)

        val vedlegg = vedleggService.finnVedleggPåBehandling(BehandlingId.random())

        Assertions.assertThat(vedlegg.first().dato).isNull()
    }
}
