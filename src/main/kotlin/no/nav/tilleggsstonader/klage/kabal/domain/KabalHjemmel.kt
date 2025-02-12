package no.nav.tilleggsstonader.klage.kabal.domain

// Se Kabals kodeverk som dette er hentet fra: https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/Hjemmel.kt
enum class KabalHjemmel(
    val id: String,
    val lovKilde: LovKilde,
    val spesifikasjon: String,
) {
    ARBML_13("ARBML_13", LovKilde.ARBEIDSMARKEDSLOVEN, "§ 13"),
    ARBML_17("ARBML_17", LovKilde.ARBEIDSMARKEDSLOVEN, "§ 17"),
    ARBML_22("ARBML_22", LovKilde.ARBEIDSMARKEDSLOVEN, "§ 22"),
    FTRL_11_A_3("FTRL_11_A_3", LovKilde.FOLKETRYGDLOVEN, "§ 11A-3"),
    FTRL_11_A_4("FTRL_11_A_4", LovKilde.FOLKETRYGDLOVEN, "§ 11A-4"),
    FTRL_11_A_4_3("FTRL_11_A_4_3", LovKilde.FOLKETRYGDLOVEN, "§ 11A-4 tredje ledd"),
    FTRL_15_11("441", LovKilde.FOLKETRYGDLOVEN, "§ 15-11"),

    FTRL_17_10("FTRL_17_10", LovKilde.FOLKETRYGDLOVEN, "§§ 17-10"),
    FTRL_17_15("FTRL_17_15", LovKilde.FOLKETRYGDLOVEN, "§§ 17-15"),
    FTRL_21_12("FTRL_21_12", LovKilde.FOLKETRYGDLOVEN, "§ 21-12"),
    FTRL_22_13("1000.022.013", LovKilde.FOLKETRYGDLOVEN, "§ 22-13"),
    FTRL_22_15("1000.022.015", LovKilde.FOLKETRYGDLOVEN, "§ 22-15"),
    FTRL_22_17A("FTRL_22_17A", LovKilde.FOLKETRYGDLOVEN, "§ 22-17a"),
    FS_TILL_ST_1_3_MOBILITET("FS_TILL_ST_1_3_MOBILITET", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 1 tredje ledd - mobilitet"),
    FS_TILL_ST_3_REISE("FS_TILL_ST_3_REISE", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 3 - reise"),
    FS_TILL_ST_6_FLYTTING("FS_TILL_ST_6_FLYTTING", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 6 - flytting"),
    FS_TILL_ST_8_BOLIG("FS_TILL_ST_8_BOLIG", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 8 - bolig"),
    FS_TILL_ST_10_TILSYN("FS_TILL_ST_10_TILSYN", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 10 - tilsyn"),
    FS_TILL_ST_12_LAEREMIDLER("FS_TILL_ST_12_LAEREMIDLER", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 12 - læremidler"),
    FS_TILL_ST_15_2("FS_TILL_ST_15_2", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 15 andre ledd"),
    FS_TILL_ST_15_3("FS_TILL_ST_15_3", LovKilde.TILLEGGSSTØNADSFORSKRIFTEN, "§ 15 tredje ledd"),
    FL_2_3("FL_2_3", LovKilde.FORELDELSESLOVEN, "§§ 2 og 3"),
    FL_10("FL_10", LovKilde.FORELDELSESLOVEN, "§ 10"),
    FVL_11("FVL_11", LovKilde.FORVALTNINGSLOVEN, "§ 11"),
    FVL_17("FVL_17", LovKilde.FORVALTNINGSLOVEN, "§ 17"),
    FVL_18_19("FVL_18_19", LovKilde.FORVALTNINGSLOVEN, "§§ 18 og 19"),
    FVL_35("FVL_35", LovKilde.FORVALTNINGSLOVEN, "§ 35"),
    FVL_41("FVL_41", LovKilde.FORVALTNINGSLOVEN, "§ 41"),
    FVL_42("FVL_42", LovKilde.FORVALTNINGSLOVEN, "§ 42"),
}
