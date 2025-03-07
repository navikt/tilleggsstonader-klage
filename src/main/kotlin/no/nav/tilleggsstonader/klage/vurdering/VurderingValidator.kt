package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto

object VurderingValidator {
    fun validerVurdering(vurdering: VurderingDto) {
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
    }
}
