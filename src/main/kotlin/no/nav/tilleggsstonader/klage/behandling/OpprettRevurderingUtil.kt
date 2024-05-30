package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype

object OpprettRevurderingUtil {

    fun skalOppretteRevurderingAutomatisk(påklagetVedtak: PåklagetVedtak): Boolean =
        påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK ||
                påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.VEDTAK
}
