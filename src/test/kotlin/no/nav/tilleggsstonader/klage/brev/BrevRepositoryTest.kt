package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.brev.domain.Brev
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerPerson
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalpost
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.brev.domain.MottakerRolle
import no.nav.tilleggsstonader.klage.felles.domain.Fil
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var brevRepository: BrevRepository

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    internal fun `lagring og henting av brev`() {
        val brev = brev()
        brevRepository.insert(brev)
        assertThat(brevRepository.findByIdOrThrow(behandling.id))
            .usingRecursiveComparison()
            .ignoringFields("sporbar")
            .isEqualTo(brev)
    }

    @Test
    internal fun oppdaterMottakerJournalpost() {
        val brev = brev()
        brevRepository.insert(brev)
        val oppdatertJournalposter = BrevmottakereJournalposter(journalposter = listOf(brevmottakereJournalpost("id")))

        brevRepository.oppdaterMottakerJournalpost(behandling.id, oppdatertJournalposter)

        val oppdatertBrev = brevRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertBrev.mottakereJournalposter).isNotEqualTo(brev.mottakereJournalposter)
        assertThat(oppdatertBrev.mottakereJournalposter).isEqualTo(oppdatertJournalposter)
    }

    private fun brev() =
        Brev(
            behandlingId = behandling.id,
            saksbehandlerHtml = "html",
            pdf = Fil("123".toByteArray()),
            mottakere =
                Brevmottakere(
                    personer =
                        listOf(
                            BrevmottakerPerson("ident", "navn", MottakerRolle.BRUKER),
                        ),
                    organisasjoner = listOf(BrevmottakerOrganisasjon("orgnr", "navn", "mottaker")),
                ),
            mottakereJournalposter = BrevmottakereJournalposter(listOf(brevmottakereJournalpost("distId"))),
        )

    private fun brevmottakereJournalpost(distribusjonId: String? = null) =
        BrevmottakereJournalpost(
            "ident",
            "journalpostId",
            distribusjonId,
        )
}
