package no.nav.tilleggsstonader.klage.journalpost

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.journalpost
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.journalpostDokument
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class JournalpostServiceTest {

    val tilleggsstønaderIntegrasjonerClientMock = mockk<TilleggsstønaderIntegrasjonerClient>()
    val journalpostService = JournalpostService(tilleggsstønaderIntegrasjonerClientMock)
    val dokumentSomPdf = "123".toByteArray()
    val dokument1 = journalpostDokument()
    val dokument2 = journalpostDokument(dokumentvarianter = null)
    val journalpost = journalpost(dokumenter = listOf(dokument1, dokument2))

    @BeforeEach
    internal fun setUp() {
        every { tilleggsstønaderIntegrasjonerClientMock.hentDokument(any(), any()) } returns dokumentSomPdf
    }

    @Test
    internal fun `skal hente ut dokument`() {
        Assertions.assertThat(journalpostService.hentDokument(journalpost, dokument1.dokumentInfoId)).isEqualTo(dokumentSomPdf)
    }

    @Test
    internal fun `skal ikke kunne hente ut dokument som ikke finnes i journalposten`() {
        assertThrows<Feil> {
            journalpostService.hentDokument(journalpost, UUID.randomUUID().toString())
        }
    }

    @Test
    internal fun `skal ikke kunne hente ut dokument som er under arbeid`() {
        assertThrows<ApiFeil> {
            journalpostService.hentDokument(journalpost, dokument2.dokumentInfoId)
        }
    }
}
