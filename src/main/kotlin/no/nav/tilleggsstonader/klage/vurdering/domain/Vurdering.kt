package no.nav.tilleggsstonader.klage.vurdering.domain

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import no.nav.tilleggsstonader.klage.kabal.domain.KabalHjemmel
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded

data class Vurdering(
    @Id
    val behandlingId: BehandlingId,
    val vedtak: Vedtak,
    @Column("arsak")
    val årsak: Årsak? = null,
    @Column("begrunnelse_omgjoring")
    val begrunnelseOmgjøring: String? = null,
    val hjemler: Hjemler? = null,
    val innstillingKlageinstans: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val interntNotat: String?,
)

enum class Vedtak {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
    ;

    fun tilBehandlingResultat(): BehandlingResultat =
        when (this) {
            OMGJØR_VEDTAK -> BehandlingResultat.MEDHOLD
            OPPRETTHOLD_VEDTAK -> BehandlingResultat.IKKE_MEDHOLD
        }
}

data class Hjemler(
    val hjemler: List<Hjemmel>,
)

enum class Hjemmel(
    val kabalHjemmel: KabalHjemmel,
    val kanBrukesForTso: Boolean,
    val kanBrukesForTsr: Boolean,
) {
    ARBML_13(KabalHjemmel.ARBML_13, true, true),
    ARBML_17(KabalHjemmel.ARBML_17, true, true),
    ARBML_22(KabalHjemmel.ARBML_22, true, true),

    FTRL_11_A_3(KabalHjemmel.FTRL_11_A_3, true, false),
    FTRL_11_A_4(KabalHjemmel.FTRL_11_A_4, true, false),
    FTRL_11_A_4_3(KabalHjemmel.FTRL_11_A_4_3, true, false),
    FTRL_15_11(KabalHjemmel.FTRL_15_11, true, false),
    FTRL_17_10(KabalHjemmel.FTRL_17_10, true, false),
    FTRL_17_15(KabalHjemmel.FTRL_17_15, true, false),
    FTRL_21_12(KabalHjemmel.FTRL_21_12, true, false),
    FTRL_22_13(KabalHjemmel.FTRL_22_13, true, false),
    FTRL_22_15(KabalHjemmel.FTRL_22_15, true, false),
    FTRL_22_17A(KabalHjemmel.FTRL_22_17A, true, false),

    FS_TILL_ST_1_3_MOBILITET(KabalHjemmel.FS_TILL_ST_1_3_MOBILITET, true, true),
    FS_TILL_ST_3_REISE(KabalHjemmel.FS_TILL_ST_3_REISE, true, true),
    FS_TILL_ST_5(KabalHjemmel.FS_TILL_ST_5, false, true),
    FS_TILL_ST_6_FLYTTING(KabalHjemmel.FS_TILL_ST_6_FLYTTING, true, true),
    FS_TILL_ST_8_BOLIG(KabalHjemmel.FS_TILL_ST_8_BOLIG, true, false),
    FS_TILL_ST_10_TILSYN(KabalHjemmel.FS_TILL_ST_10_TILSYN, true, false),
    FS_TILL_ST_12_LAEREMIDLER(KabalHjemmel.FS_TILL_ST_12_LAEREMIDLER, true, false),
    FS_TILL_ST_15_2(KabalHjemmel.FS_TILL_ST_15_2, true, true),
    FS_TILL_ST_15_3(KabalHjemmel.FS_TILL_ST_15_3, true, true),

    FL_2_3(KabalHjemmel.FL_2_3, true, true),
    FL_10(KabalHjemmel.FL_10, true, true),

    FVL_11(KabalHjemmel.FVL_11, true, true),
    FVL_17(KabalHjemmel.FVL_17, true, true),
    FVL_18_19(KabalHjemmel.FVL_18_19, true, true),
    FVL_35(KabalHjemmel.FVL_35, true, true),
    FVL_41(KabalHjemmel.FVL_41, true, true),
    FVL_42(KabalHjemmel.FVL_42, true, true),
}
