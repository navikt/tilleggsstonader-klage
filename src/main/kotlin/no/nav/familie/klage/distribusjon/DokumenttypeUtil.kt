package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

object DokumenttypeUtil {

    fun dokumenttypeBrev(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV // TODO: Legg til klagebrev
        }

    fun dokumenttypeSaksbehandlingsblankett(stønadstype: Stønadstype): Dokumenttype = when (stønadstype) {
        Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV // TODO: Legg til blankett for klage
    }
}
