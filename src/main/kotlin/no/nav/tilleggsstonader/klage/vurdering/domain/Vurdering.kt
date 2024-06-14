package no.nav.tilleggsstonader.klage.vurdering.domain

import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import no.nav.tilleggsstonader.klage.kabal.KabalHjemmel
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vurdering(
    @Id
    val behandlingId: UUID,
    val vedtak: Vedtak,
    @Column("arsak")
    val årsak: Årsak? = null,
    @Column("begrunnelse_omgjoring")
    val begrunnelseOmgjøring: String? = null,
    val hjemmel: Hjemmel? = null,
    val innstillingKlageinstans: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val interntNotat: String?,
)

enum class Vedtak {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
    ;

    fun tilBehandlingResultat(): BehandlingResultat {
        return when (this) {
            OMGJØR_VEDTAK -> BehandlingResultat.MEDHOLD
            OPPRETTHOLD_VEDTAK -> BehandlingResultat.IKKE_MEDHOLD
        }
    }
}

enum class Hjemmel(val kabalHjemmel: KabalHjemmel) {
    ARBML_13(KabalHjemmel.ARBML_13),
    ARBML_17(KabalHjemmel.ARBML_17),
    ARBML_22(KabalHjemmel.ARBML_22),
    FTRL_11_A_3(KabalHjemmel.FTRL_11_A_3),
    FTRL_11_A_4(KabalHjemmel.FTRL_11_A_4),
    FTRL_11_A_4_3(KabalHjemmel.FTRL_11_A_4_3),
    FTRL_15_11(KabalHjemmel.FTRL_15_11),
    FTRL_17_10_17_15(KabalHjemmel.FTRL_17_10_17_15),
    FTRL_21_12(KabalHjemmel.FTRL_21_12),
    FTRL_22_13(KabalHjemmel.FTRL_22_13),
    FTRL_22_15(KabalHjemmel.FTRL_22_15),
    FTRL_22_17A(KabalHjemmel.FTRL_22_17A),
    FS_TILL_ST_1_3_MOBILITET(KabalHjemmel.FS_TILL_ST_1_3_MOBILITET),
    FS_TILL_ST_3_REISE(KabalHjemmel.FS_TILL_ST_3_REISE),
    FS_TILL_ST_6_FLYTTING(KabalHjemmel.FS_TILL_ST_6_FLYTTING),
    FS_TILL_ST_8_BOLIG(KabalHjemmel.FS_TILL_ST_8_BOLIG),
    FS_TILL_ST_10_TILSYN(KabalHjemmel.FS_TILL_ST_10_TILSYN),
    FS_TILL_ST_12_LAEREMIDLER(KabalHjemmel.FS_TILL_ST_12_LAEREMIDLER),
    FS_TILL_ST_15_2(KabalHjemmel.FS_TILL_ST_15_2),
    FS_TILL_ST_15_3(KabalHjemmel.FS_TILL_ST_15_3),
    FL_2_3(KabalHjemmel.FL_2_3),
    FL_10(KabalHjemmel.FL_10),
    FVL_11(KabalHjemmel.FVL_11),
    FVL_17(KabalHjemmel.FVL_17),
    FVL_18_19(KabalHjemmel.FVL_18_19),
    FVL_35(KabalHjemmel.FVL_35),
    FVL_41(KabalHjemmel.FVL_41),
    FVL_42(KabalHjemmel.FVL_42),
}
