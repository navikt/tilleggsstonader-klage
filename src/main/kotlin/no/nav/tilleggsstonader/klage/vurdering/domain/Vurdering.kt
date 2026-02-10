package no.nav.tilleggsstonader.klage.vurdering.domain

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import no.nav.tilleggsstonader.klage.kabal.domain.KabalHjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel.Companion.Hjemmeltema.TSO
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel.Companion.Hjemmeltema.TSR
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
    val relevantForTemaer: Set<Hjemmeltema>,
) {
    ARBML_13(KabalHjemmel.ARBML_13, setOf(TSO, TSR)),
    ARBML_17(KabalHjemmel.ARBML_17, setOf(TSO, TSR)),
    ARBML_22(KabalHjemmel.ARBML_22, setOf(TSO, TSR)),

    FTRL_11_A_3(KabalHjemmel.FTRL_11_A_3, setOf(TSO)),
    FTRL_11_A_4(KabalHjemmel.FTRL_11_A_4, setOf(TSO)),
    FTRL_11_A_4_3(KabalHjemmel.FTRL_11_A_4_3, setOf(TSO)),
    FTRL_15_11(KabalHjemmel.FTRL_15_11, setOf(TSO)),
    FTRL_17_10(KabalHjemmel.FTRL_17_10, setOf(TSO)),
    FTRL_17_15(KabalHjemmel.FTRL_17_15, setOf(TSO)),
    FTRL_21_12(KabalHjemmel.FTRL_21_12, setOf(TSO)),
    FTRL_22_13(KabalHjemmel.FTRL_22_13, setOf(TSO)),
    FTRL_22_15(KabalHjemmel.FTRL_22_15, setOf(TSO)),
    FTRL_22_17A(KabalHjemmel.FTRL_22_17A, setOf(TSO)),

    FS_TILL_ST_1_3_MOBILITET(KabalHjemmel.FS_TILL_ST_1_3_MOBILITET, setOf(TSO, TSR)),
    FS_TILL_ST_3_REISE(KabalHjemmel.FS_TILL_ST_3_REISE, setOf(TSO, TSR)),
    FS_TILL_ST_5(KabalHjemmel.FS_TILL_ST_5, setOf(TSR)),
    FS_TILL_ST_6_FLYTTING(KabalHjemmel.FS_TILL_ST_6_FLYTTING, setOf(TSO, TSR)),
    FS_TILL_ST_8_BOLIG(KabalHjemmel.FS_TILL_ST_8_BOLIG, setOf(TSO)),
    FS_TILL_ST_10_TILSYN(KabalHjemmel.FS_TILL_ST_10_TILSYN, setOf(TSO)),
    FS_TILL_ST_12_LAEREMIDLER(KabalHjemmel.FS_TILL_ST_12_LAEREMIDLER, setOf(TSO)),
    FS_TILL_ST_15_2(KabalHjemmel.FS_TILL_ST_15_2, setOf(TSO, TSR)),
    FS_TILL_ST_15_3(KabalHjemmel.FS_TILL_ST_15_3, setOf(TSO, TSR)),

    FL_2_3(KabalHjemmel.FL_2_3, setOf(TSO, TSR)),
    FL_10(KabalHjemmel.FL_10, setOf(TSO, TSR)),

    FVL_11(KabalHjemmel.FVL_11, setOf(TSO, TSR)),
    FVL_17(KabalHjemmel.FVL_17, setOf(TSO, TSR)),
    FVL_18_19(KabalHjemmel.FVL_18_19, setOf(TSO, TSR)),
    FVL_35(KabalHjemmel.FVL_35, setOf(TSO, TSR)),
    FVL_41(KabalHjemmel.FVL_41, setOf(TSO, TSR)),
    FVL_42(KabalHjemmel.FVL_42, setOf(TSO, TSR)),

    ;

    companion object {
        enum class Hjemmeltema {
            TSO,
            TSR,
        }
    }
}
