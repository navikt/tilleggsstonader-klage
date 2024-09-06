package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

object DokumenttypeUtil {

    fun dokumenttypeBrev(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV // TODO: Legg til klagebrev
            Stønadstype.LÆREMIDLER -> error("TODO: Funksjonaliteten er ikke implementert for LÆREMIDLER enda")
        }

    fun dokumenttypeSaksbehandlingsblankett(stønadstype: Stønadstype): Dokumenttype = when (stønadstype) {
        Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV // TODO: Legg til blankett for klage
        Stønadstype.LÆREMIDLER -> error("TODO: Funksjonaliteten er ikke implementert for LÆREMIDLER enda")
    }
}
