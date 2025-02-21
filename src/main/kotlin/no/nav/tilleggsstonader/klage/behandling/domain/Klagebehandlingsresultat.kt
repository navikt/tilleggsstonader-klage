package no.nav.tilleggsstonader.klage.behandling.domain

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.HenlagtÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansResultatDto
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Aggregering av behandling, fagsak, vedtak for å hente ut relevant informasjon i en spørring
 */
data class Klagebehandlingsresultat(
    val id: BehandlingId,
    val fagsakId: UUID,
    val fagsakPersonId: UUID,
    val status: BehandlingStatus,
    val opprettet: LocalDateTime,
    val mottattDato: LocalDate,
    val resultat: BehandlingResultat,
    @Column("arsak")
    val årsak: Årsak?,
    val vedtaksdato: LocalDateTime?,
    @Column("henlagt_arsak")
    val henlagtÅrsak: HenlagtÅrsak?,
    val henlagtBegrunnelse: String?,
)

fun Klagebehandlingsresultat.tilEksternKlagebehandlingDto(klageinstansResultat: List<KlageinstansResultatDto>) =
    KlagebehandlingDto(
        id = this.id.id,
        fagsakId = this.fagsakId,
        status = this.status,
        opprettet = this.opprettet,
        mottattDato = this.mottattDato,
        resultat = this.resultat,
        årsak = this.årsak,
        vedtaksdato = this.vedtaksdato,
        klageinstansResultat = klageinstansResultat,
        henlagtÅrsak = this.henlagtÅrsak,
        henlagtBegrunnelse = this.henlagtBegrunnelse,
    )
