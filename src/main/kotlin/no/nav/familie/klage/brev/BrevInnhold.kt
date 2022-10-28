package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.BrevInnhold.lesMerUrl
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.kontrakter.felles.klage.Stønadstype

object BrevInnhold {

    fun lagOpprettholdelseBrev(
        ident: String,
        instillingKlageinstans: String,
        navn: String,
        stønadstype: Stønadstype
    ): FritekstBrevRequestDto {
        val stønadstypeVisningsnavn = stønadstype.tilVisningsnavn()
        val lesMerUrl = stønadstype.lesMerUrl()

        return FritekstBrevRequestDto(
            overskrift = "",
            navn = navn,
            personIdent = ident,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Vi har sendt klagen din til NAV Klageinstans"
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Vi har vurdert klagen din på vedtaket om $stønadstypeVisningsnavn, og kommet frem til at vedtaket ikke endres. NAV Klageinstans skal derfor vurdere saken din på nytt."
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider."
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Dette er vurderingen vi har sendt til NAV Klageinstans\n$instillingKlageinstans"
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via nav.no/klage."
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Har du spørsmål?\nDu finner informasjon som kan være nyttig for deg på $lesMerUrl. Du kan også kontakte oss på nav.no/kontakt."
                )
            )
        )
    }

    fun lagFormkravAvvistBrev(ident: String, navn: String, begrunnelse: String, stønadstype: Stønadstype): FritekstBrevRequestDto {
        val lesMerUrl = stønadstype.lesMerUrl()

        return FritekstBrevRequestDto(
            overskrift = "",
            personIdent = ident,
            navn = navn,
            avsnitt = listOf(

                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    begrunnelse
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via nav.no/klage."
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Har du spørsmål?\nDu finner informasjon som kan være nyttig for deg på $lesMerUrl. Du kan også kontakte oss på nav.no/kontakt."
                )
            )
        )
    }

    private fun Stønadstype.tilVisningsnavn() = this.toString().lowercase().replaceFirstChar { it.uppercase() }

    private fun Stønadstype.lesMerUrl() = when (this) {
        Stønadstype.OVERGANGSSTØNAD,
        Stønadstype.BARNETILSYN,
        Stønadstype.SKOLEPENGER -> "nav.no/familie/alene-med-barn"
        Stønadstype.BARNETRYGD -> "nav.no/barnetrygd"
        Stønadstype.KONTANTSTØTTE -> "nav.no/kontantstotte"
    }
}
