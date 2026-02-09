package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto
import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet

object VurderingValidator {
    fun validerVurdering(
        vurdering: VurderingDto,
        stønadstype: Stønadstype,
    ) {
        when (vurdering.vedtak) {
            Vedtak.OMGJØR_VEDTAK -> {
                feilHvis(vurdering.årsak == null) {
                    "Mangler årsak på omgjør vedtak"
                }
                feilHvis(vurdering.begrunnelseOmgjøring == null) {
                    "Mangler begrunnelse for omgjøring på omgjør vedtak"
                }
                feilHvis(!vurdering.hjemler.isNullOrEmpty()) {
                    "Kan ikke lagre hjemmel på omgjør vedtak"
                }
                feilHvis(vurdering.innstillingKlageinstans != null) {
                    "Skal ikke ha innstilling til klageinstans ved omgjøring av vedtak"
                }
            }

            Vedtak.OPPRETTHOLD_VEDTAK -> {
                feilHvis(vurdering.hjemler.isNullOrEmpty()) {
                    "Mangler hjemmel på oppretthold vedtak"
                }
                feilHvis(vurdering.årsak != null) {
                    "Kan ikke lagre årsak på oppretthold vedtak"
                }
                feilHvis(vurdering.begrunnelseOmgjøring != null) {
                    "Kan ikke lagre begrunnelse på oppretthold vedtak"
                }
                feilHvis(vurdering.innstillingKlageinstans.isNullOrBlank()) {
                    "Må skrive innstilling til klageinstans ved opprettholdelse av vedtak"
                }
            }
        }

        validerRettHjemlerForEnhet(vurdering, stønadstype)
    }

    private fun validerRettHjemlerForEnhet(
        vurdering: VurderingDto,
        stønadstype: Stønadstype,
    ) {
        when (val behandlendeEnhet = stønadstype.behandlendeEnhet()) {
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD ->
                vurdering.hjemler?.let { hjemler ->
                    feilHvis(hjemler.any { !it.kanBrukesForTso }) {
                        "En eller flere hjemler kan ikke brukes når behandlende enhet er Nav Arbeid og Ytelser: ${hjemler.joinToString()}"
                    }
                }

            Enhet.NAV_TILTAK_OSLO -> {
                vurdering.hjemler?.let { hjemler ->
                    feilHvis(hjemler.any { !it.kanBrukesForTsr }) {
                        "En eller flere hjemler kan ikke brukes når behandlende enhet er Nav Tiltak Oslo: ${hjemler.joinToString()}"
                    }
                }
            }

            Enhet.NAV_ARBEID_OG_YTELSER_EGNE_ANSATTE,
            Enhet.NAV_EGNE_ANSATTE_OSLO,
            Enhet.VIKAFOSSEN,
            -> error("Kann ikke behandle klage når behandlende enhet er : $behandlendeEnhet")
        }
    }
}
