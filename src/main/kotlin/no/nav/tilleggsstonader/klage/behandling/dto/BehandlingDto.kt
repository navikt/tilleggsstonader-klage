package no.nav.tilleggsstonader.klage.behandling.dto

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.IKKE_VALGT
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansResultatDto
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val fagsakId: UUID,
    val steg: StegType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDateTime? = null,
    val stønadstype: Stønadstype,
    val klageinstansResultat: List<KlageinstansResultatDto>,
    val påklagetVedtak: PåklagetVedtakDto,
    val eksternFagsystemFagsakId: String,
    val fagsystem: Fagsystem,
    val klageMottatt: LocalDate,
    val fagsystemRevurdering: FagsystemRevurdering?,
)

/**
 * @param fagsystemVedtak skal ikke brukes ved innsending, men kun når vi sender ut data
 */
data class PåklagetVedtakDto(
    val eksternFagsystemBehandlingId: String?,
    val påklagetVedtakstype: PåklagetVedtakstype,
    val fagsystemVedtak: FagsystemVedtak? = null,
    val manuellVedtaksdato: LocalDate? = null,
    val regelverk: Regelverk? = null,
) {
    fun erGyldig(): Boolean = when (påklagetVedtakstype) {
        VEDTAK, PåklagetVedtakstype.TILBAKEKREVING -> eksternFagsystemBehandlingId != null
        PåklagetVedtakstype.UTEN_VEDTAK, IKKE_VALGT -> eksternFagsystemBehandlingId == null
    }

    fun harTattStillingTil(): Boolean = påklagetVedtakstype != IKKE_VALGT
}

fun Behandling.tilDto(fagsak: Fagsak, klageinstansResultat: List<KlageinstansResultatDto>): BehandlingDto =
    BehandlingDto(
        id = this.id,
        fagsakId = this.fagsakId,
        steg = this.steg,
        status = this.status,
        sistEndret = this.sporbar.endret.endretTid,
        resultat = this.resultat,
        opprettet = this.sporbar.opprettetTid,
        stønadstype = fagsak.stønadstype,
        fagsystem = fagsak.fagsystem,
        eksternFagsystemFagsakId = fagsak.eksternId,
        klageinstansResultat = klageinstansResultat,
        påklagetVedtak = this.påklagetVedtak.tilDto(),
        klageMottatt = this.klageMottatt,
        fagsystemRevurdering = this.fagsystemRevurdering,
    )
