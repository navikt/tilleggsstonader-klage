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

    @Test
    internal fun `brev for avvist formkrav uten rettslig interesse skal føre til eget avvisnings brev`() {
        val stønadstype = Stønadstype.BARNETILSYN
        val brev =
            lagFormkravAvvistBrev(
                "123456789",
                "instilling abc",
                ikkeOppfyltRettsligInteresse(),
                stønadstype,
            )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om ${stønadstype.visningsnavn}")
        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Etter at du sendte inn klagen, har du fått innvilget og " +
                "utbetalt ${ stønadstype.visningsnavn } " +
                "for samme periode som klagen gjelder. " +
                "Klagebehandlingen kan derfor ikke føre til et annet resultat for deg. " +
                "Fordi du ikke lenger har et reelt behov for å få klagen behandlet, " +
                "har du ikke rettslig klageinteresse, som er et vilkår for å få klagen behandlet. Klagen blir derfor avvist.",
        )
        assertThat(brev.avsnitt.elementAt(1).innhold).isEqualTo("brevtekst")
        assertThat(brev.avsnitt.elementAt(2).innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 34.")
        assertThat(brev.avsnitt.elementAt(3).deloverskrift).isEqualTo("Du har rett til å klage")
        assertThat(brev.avsnitt.elementAt(4).deloverskrift).isEqualTo("Du har rett til innsyn")
        assertThat(brev.avsnitt.elementAt(5).deloverskrift).isEqualTo("Har du spørsmål?")
        assertThat(brev.avsnitt.size).isEqualTo(6)
    }

    @Test
    internal fun `hvis formkrav om rettslig interesse er blandt de som ikke er oppfylt skal føre til rettslig interesse avvisnings brev`() {
        val stønadstype = Stønadstype.BARNETILSYN
        val brev =
            lagFormkravAvvistBrev(
                "123456789",
                "instilling abc",
                oppfyltForm(BehandlingId.random())
                    .copy(
                        klagersRettsligInteresse = FormVilkår.IKKE_OPPFYLT,
                        klagePart = FormVilkår.IKKE_OPPFYLT,
                        brevtekst = "brevtekst",
                    ),
                stønadstype,
            )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om ${stønadstype.visningsnavn}")
        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Etter at du sendte inn klagen, har du fått innvilget og " +
                "utbetalt ${ stønadstype.visningsnavn } " +
                "for samme periode som klagen gjelder. " +
                "Klagebehandlingen kan derfor ikke føre til et annet resultat for deg. " +
                "Fordi du ikke lenger har et reelt behov for å få klagen behandlet, " +
                "har du ikke rettslig klageinteresse, som er et vilkår for å få klagen behandlet. Klagen blir derfor avvist.",
        )
        assertThat(brev.avsnitt.elementAt(1).innhold).isEqualTo("brevtekst")
        assertThat(brev.avsnitt.elementAt(2).innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 34.")
        assertThat(brev.avsnitt.elementAt(3).deloverskrift).isEqualTo("Du har rett til å klage")
        assertThat(brev.avsnitt.elementAt(4).deloverskrift).isEqualTo("Du har rett til innsyn")
        assertThat(brev.avsnitt.elementAt(5).deloverskrift).isEqualTo("Har du spørsmål?")
        assertThat(brev.avsnitt.size).isEqualTo(6)
    }

    private fun ikkeOppfyltRettsligInteresse() =
        oppfyltForm(BehandlingId.random())
            .copy(klagersRettsligInteresse = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst")

    private fun ikkeOppfyltForm() =
        oppfyltForm(BehandlingId.random())
            .copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst")
}
