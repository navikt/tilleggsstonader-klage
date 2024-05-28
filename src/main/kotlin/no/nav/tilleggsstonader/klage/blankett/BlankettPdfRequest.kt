package no.nav.tilleggsstonader.klage.blankett

import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.domain.FormkravFristUnntak
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import java.time.LocalDate
import java.time.LocalDateTime

data class BlankettPdfRequest(
    val behandling: BlankettPdfBehandling,
    val personopplysninger: PersonopplysningerDto,
    val formkrav: BlankettFormDto,
    val vurdering: BlankettVurderingDto?,
)

data class BlankettPdfBehandling(
    val eksternFagsakId: String,
    val stønadstype: Stønadstype,
    val klageMottatt: LocalDate,
    val resultat: BehandlingResultat,
    val påklagetVedtak: BlankettPåklagetVedtakDto?,
)

data class BlankettPåklagetVedtakDto(
    val behandlingstype: String,
    val resultat: String,
    val vedtakstidspunkt: LocalDateTime,
)

data class PersonopplysningerDto(
    val navn: String,
    val personIdent: String,
)

data class BlankettFormDto(
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klagefristOverholdtUnntak: FormkravFristUnntak?,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String?,
    val brevtekst: String?,
)

data class BlankettVurderingDto(
    val vedtak: Vedtak,
    val årsak: Årsak?,
    val begrunnelseOmgjøring: String?,
    val hjemmel: Hjemmel?,
    val innstillingKlageinstans: String?,
    val interntNotat: String?,
)
