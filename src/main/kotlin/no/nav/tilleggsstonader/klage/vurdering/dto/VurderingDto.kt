package no.nav.tilleggsstonader.klage.vurdering.dto

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.kontrakter.klage.Årsak

data class VurderingDto(
    val behandlingId: BehandlingId,
    val vedtak: Vedtak,
    val årsak: Årsak? = null,
    val begrunnelseOmgjøring: String? = null,
    val hjemmel: Hjemmel? = null,
    val innstillingKlageinstans: String? = null,
    val interntNotat: String?,
)

fun Vurdering.tilDto(): VurderingDto =
    VurderingDto(
        behandlingId = this.behandlingId,
        vedtak = this.vedtak,
        årsak = this.årsak,
        begrunnelseOmgjøring = this.begrunnelseOmgjøring,
        hjemmel = this.hjemmel,
        innstillingKlageinstans = this.innstillingKlageinstans,
        interntNotat = this.interntNotat,
    )
