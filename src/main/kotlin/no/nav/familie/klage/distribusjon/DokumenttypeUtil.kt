package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype

object DokumenttypeUtil {

    fun dokumenttypeBrev(stønadstype: Stønadstype): Dokumenttype = when (stønadstype) {
        Stønadstype.OVERGANGSSTØNAD -> Dokumenttype.KLAGE_VEDTAKSBREV_OVERGANGSSTØNAD
        Stønadstype.BARNETILSYN -> Dokumenttype.KLAGE_VEDTAKSBREV_BARNETILSYN
        Stønadstype.SKOLEPENGER -> Dokumenttype.KLAGE_VEDTAKSBREV_SKOLEPENGER
        Stønadstype.BARNETRYGD -> Dokumenttype.KLAGE_VEDTAKSBREV_BARNETRYGD
        Stønadstype.KONTANTSTØTTE -> Dokumenttype.KLAGE_VEDTAKSBREV_KONTANTSTØTTE
    }

    fun dokumenttypeSaksbehandlingsblankett(stønadstype: Stønadstype): Dokumenttype = when (stønadstype) {
        Stønadstype.OVERGANGSSTØNAD -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_OVERGANGSSTØNAD
        Stønadstype.BARNETILSYN -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_BARNETILSYN
        Stønadstype.SKOLEPENGER -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_SKOLEPENGER
        Stønadstype.BARNETRYGD -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_BARNETRYGD
        Stønadstype.KONTANTSTØTTE -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_KONTANTSTØTTE
    }
}
