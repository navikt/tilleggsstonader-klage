package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.kontrakter.felles.klage.FagsystemType

object OpprettRevurderingUtil {

    private val fagsystemstyper = setOf(
        FagsystemType.ORDNIÆR,
        FagsystemType.SANKSJON_1_MND,
    )

    fun skalOppretteRevurderingAutomatisk(påklagetVedtak: PåklagetVedtak): Boolean =
        påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK ||
            erVedtakIFagsystem(påklagetVedtak)

    private fun erVedtakIFagsystem(påklagetVedtak: PåklagetVedtak) =
        påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.VEDTAK &&
            fagsystemstyper.contains(påklagetVedtak.påklagetVedtakDetaljer?.fagsystemType)
}
