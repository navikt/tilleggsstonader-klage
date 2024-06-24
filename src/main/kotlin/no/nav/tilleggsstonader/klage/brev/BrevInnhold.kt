package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.brev.FormBrevUtil.utledIkkeOppfylteFormkrav
import no.nav.tilleggsstonader.klage.brev.FormBrevUtil.utledLovtekst
import no.nav.tilleggsstonader.klage.brev.FormBrevUtil.utledÅrsakTilAvvisningstekst
import no.nav.tilleggsstonader.klage.brev.dto.AvsnittDto
import no.nav.tilleggsstonader.klage.brev.dto.FritekstBrevRequestDto
import no.nav.tilleggsstonader.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import no.nav.tilleggsstonader.klage.felles.util.TekstUtil.norskFormat
import no.nav.tilleggsstonader.klage.formkrav.domain.Form
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import java.time.LocalDate

object BrevInnhold {

    fun lagOpprettholdelseBrev(
        ident: String,
        instillingKlageinstans: String,
        navn: String,
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto {
        return FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til NAV Klageinstans Sør",
            navn = navn,
            personIdent = ident,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Vi har ${klageMottatt.norskFormat()} fått klagen din på vedtaket om " +
                        "${stønadstype.visningsnavn()} som ble gjort " +
                        "${påklagetVedtakDetaljer.vedtakstidspunkt.norskFormat()}, " +
                        "og kommet frem til at vi ikke endrer vedtaket. NAV Klageinstans skal derfor vurdere saken din på nytt.",
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.",
                ),
                AvsnittDto(
                    deloverskrift = "Dette er vurderingen vi har sendt til NAV Klageinstans:",
                    innhold = instillingKlageinstans,
                ),
                AvsnittDto(
                    deloverskrift = "Har du nye opplysninger?",
                    innhold =
                    "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${stønadstype.klageUrl()}.",
                ),
                harDuSpørsmålAvsnitt(stønadstype),
            ),
        )
    }

    fun lagFormkravAvvistBrev(
        ident: String,
        navn: String,
        formkrav: Form,
        stønadstype: Stønadstype,
    ): FritekstBrevRequestDto {
        val ikkeOppfylteFormkrav = utledIkkeOppfylteFormkrav(formkrav)
        val brevtekstFraSaksbehandler =
            formkrav.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din på vedtaket om ${stønadstype.visningsnavn()}",
            personIdent = ident,
            navn = navn,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold = utledÅrsakTilAvvisningstekst(ikkeOppfylteFormkrav),
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = brevtekstFraSaksbehandler,
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = utledLovtekst(ikkeOppfylteFormkrav),
                ),
                duHarRettTilÅKlageAvsnitt(stønadstype),
                AvsnittDto(
                    deloverskrift = "Du har rett til innsyn",
                    innhold =
                    "På nav.no/dittnav kan du se dokumentene i saken din.",
                ),
                harDuSpørsmålAvsnitt(stønadstype),
            ),
        )
    }

    fun lagFormkravAvvistBrevIkkePåklagetVedtak(
        ident: String,
        navn: String,
        formkrav: Form,
        stønadstype: Stønadstype,
    ): FritekstBrevRequestDto {
        val brevtekstFraSaksbehandler =
            formkrav.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din",
            personIdent = ident,
            navn = navn,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Vi har avvist klagen din fordi du ikke har klaget på et vedtak.",
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = brevtekstFraSaksbehandler,
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.",
                ),
                duHarRettTilÅKlageAvsnitt(stønadstype),
                AvsnittDto(
                    deloverskrift = "Du har rett til innsyn",
                    innhold =
                    "På nav.no/dittnav kan du se dokumentene i saken din.",
                ),
                harDuSpørsmålAvsnitt(stønadstype),
            ),
        )
    }

    private fun duHarRettTilÅKlageAvsnitt(stønadstype: Stønadstype) = AvsnittDto(
        deloverskrift = "Du har rett til å klage",
        innhold =
        "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
            "Du finner skjema og informasjon på ${stønadstype.klageUrl()}.",
    )

    private fun harDuSpørsmålAvsnitt(stønadstype: Stønadstype) = AvsnittDto(
        deloverskrift = "Har du spørsmål?",
        innhold =
        "Du finner mer informasjon på ${stønadstype.lesMerUrl()}.\n\n" +
            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
    )

    private fun Stønadstype.lesMerUrl() = when (this) {
        Stønadstype.BARNETILSYN -> "nav.no/alene-med-barn"
    }

    private fun Stønadstype.klageUrl() = when (this) {
        Stønadstype.BARNETILSYN -> "klage.nav.no/nb/ettersendelse/klage/TILLEGGSSTONADER/begrunnelse"
    }
}
