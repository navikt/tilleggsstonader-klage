package no.nav.tilleggsstonader.klage.distribusjon

import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

object DokumenttypeUtil {
    fun dokumenttypeVedtaksbrev(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_KLAGE_VEDTAKSBREV
            Stønadstype.LÆREMIDLER -> Dokumenttype.LÆREMIDLER_KLAGE_VEDTAKSBREV
        }

    fun dokumenttypeInterntVedtak(stønadstype: Stønadstype): Dokumenttype =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_INTERNT_VEDTAK
            Stønadstype.LÆREMIDLER -> Dokumenttype.LÆREMIDLER_INTERNT_VEDTAK
        }
}
