package no.nav.tilleggsstonader.klage.behandling.dto

import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.domain.harManuellVedtaksdato
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak

fun FagsystemVedtak.tilPåklagetVedtakDetaljer() = PåklagetVedtakDetaljer(
    behandlingstype = this.behandlingstype,
    eksternFagsystemBehandlingId = this.eksternBehandlingId,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt,
    fagsystemType = this.fagsystemType,
    regelverk = this.regelverk,

)

fun PåklagetVedtakDetaljer.tilFagsystemVedtak() = FagsystemVedtak(
    behandlingstype = this.behandlingstype,
    eksternBehandlingId = this.eksternFagsystemBehandlingId ?: "",
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt,
    fagsystemType = this.fagsystemType,
    regelverk = this.regelverk,
)

fun PåklagetVedtak.tilDto(): PåklagetVedtakDto =
    PåklagetVedtakDto(
        eksternFagsystemBehandlingId = this.påklagetVedtakDetaljer?.eksternFagsystemBehandlingId,
        påklagetVedtakstype = this.påklagetVedtakstype,
        fagsystemVedtak = this.påklagetVedtakDetaljer?.tilFagsystemVedtak(),
        manuellVedtaksdato = if (påklagetVedtakstype.harManuellVedtaksdato()) this.påklagetVedtakDetaljer?.vedtakstidspunkt?.toLocalDate() else null,
    )
