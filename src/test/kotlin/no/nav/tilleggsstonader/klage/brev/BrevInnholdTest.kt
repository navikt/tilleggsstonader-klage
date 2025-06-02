package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.BrevInnhold.lagFormkravAvvistBrev
import no.nav.tilleggsstonader.klage.brev.BrevInnhold.lagFormkravAvvistBrevIkkePåklagetVedtak
import no.nav.tilleggsstonader.klage.brev.BrevInnhold.lagOpprettholdelseBrev
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.oppfyltForm
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class BrevInnholdTest {
    private val mottattDato = LocalDate.of(2020, 1, 1)
    private val vedtakstidspunkt = LocalDateTime.of(2021, 11, 5, 14, 56, 22)

    @Test
    internal fun `brev for opprettholdelse skal inneholde dato og stønadstype`() {
        val brev =
            lagOpprettholdelseBrev(
                "123456789",
                "Innstilling abc",
                "Navn Navnesen",
                Stønadstype.BARNETILSYN,
                påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt),
                mottattDato,
            )

        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Vi har 1.januar 2020 fått klagen din på vedtaket om stønad til pass av barn som ble gjort 5.november 2021, " +
                "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt.",
        )
    }

    @Test
    internal fun `brev for avvist formkrav skal inneholde dato og stønadstype`() {
        val brev =
            lagFormkravAvvistBrev(
                "123456789",
                "Innstilling abc",
                ikkeOppfyltForm(),
                Stønadstype.BARNETILSYN,
            )

        assertThat(brev.overskrift).isEqualTo(
            "Vi har avvist klagen din på vedtaket om stønad til pass av barn",
        )
    }

    @Test
    internal fun `brev for avvist formkrav uten påklaget vedtak skal føre til et eget avvisningsbrev`() {
        val brev =
            lagFormkravAvvistBrevIkkePåklagetVedtak(
                "123456789",
                "Innstilling abc",
                ikkeOppfyltForm(),
                Stønadstype.BARNETILSYN,
            )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din")
        assertThat(brev.avsnitt.first().innhold).isEqualTo("Vi har avvist klagen din fordi du ikke har klaget på et vedtak.")
        assertThat(brev.avsnitt.elementAt(1).innhold).isEqualTo("brevtekst")
        assertThat(brev.avsnitt.elementAt(2).innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
        assertThat(brev.avsnitt.elementAt(3).deloverskrift).isEqualTo("Du har rett til å klage")
        assertThat(brev.avsnitt.elementAt(4).deloverskrift).isEqualTo("Du har rett til innsyn")
        assertThat(brev.avsnitt.elementAt(5).deloverskrift).isEqualTo("Har du spørsmål?")
        assertThat(brev.avsnitt.size).isEqualTo(6)
    }

    private fun ikkeOppfyltForm() =
        oppfyltForm(BehandlingId.random())
            .copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst")
}
