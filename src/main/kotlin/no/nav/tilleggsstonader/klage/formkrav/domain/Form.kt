package no.nav.tilleggsstonader.klage.formkrav.domain

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded

data class Form(
    @Id
    val behandlingId: BehandlingId,
    val klagePart: FormVilkår = FormVilkår.IKKE_SATT,
    val klagefristOverholdt: FormVilkår = FormVilkår.IKKE_SATT,
    val klagefristOverholdtUnntak: FormkravFristUnntak = FormkravFristUnntak.IKKE_SATT,
    val klageKonkret: FormVilkår = FormVilkår.IKKE_SATT,
    val klageSignert: FormVilkår = FormVilkår.IKKE_SATT,
    val saksbehandlerBegrunnelse: String? = null,
    val brevtekst: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class FormkravFristUnntak {
    UNNTAK_KAN_IKKE_LASTES,
    UNNTAK_SÆRLIG_GRUNN,
    IKKE_UNNTAK,
    IKKE_SATT,
}

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT,
}
