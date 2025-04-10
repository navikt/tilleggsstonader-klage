package no.nav.tilleggsstonader.klage.behandlingsstatistikk

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkHendelse.FERDIG
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkHendelse.SENDT_TIL_KA
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.personopplysninger.PersonopplysningerService
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingKlageDvh
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.SakYtelseDvh
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

enum class BehandlingsstatistikkHendelse {
    MOTTATT,
    PÅBEGYNT,
    VENTER,
    FERDIG,
    SENDT_TIL_KA,
}

@Service
class BehandlingsstatistikkService(
    private val behandlingsstatistikkProducer: BehandlingsstatistikkProducer,
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
) {
    @Transactional
    fun sendBehandlingstatistikk(
        behandlingsId: BehandlingId,
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?,
    ) {
        val behandlingsstatistikkKlage =
            mapTilBehandlingStatistikk(behandlingsId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler)
        behandlingsstatistikkProducer.sendBehandlingsstatistikk(behandlingsstatistikkKlage)
    }

    // TODO: Denne mangler anti-corruption layer. Hvis vi f.eks. endrer på BehandlingResultat.Årsak-enumen, vil kontrakten med DVH påvirkes.
    private fun mapTilBehandlingStatistikk(
        behandlingId: BehandlingId,
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?,
    ): BehandlingKlageDvh {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandling.id)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val erStrengtFortrolig =
            personopplysningerService.hentPersonopplysninger(behandlingId).adressebeskyttelse?.erStrengtFortrolig() == true

        val behandlendeEnhet =
            maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig,
                behandling.behandlendeEnhet,
            )

        val påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer

        return BehandlingKlageDvh(
            behandlingId = behandling.eksternBehandlingId.toString(),
            behandlingUuid = behandling.eksternBehandlingId.toString(),
            saksnummer = fagsak.eksternId,
            sakId = fagsak.eksternId,
            aktorId = fagsak.hentAktivIdent(),
            registrertTid = behandling.sporbar.opprettetTid,
            endretTid = hendelseTidspunkt,
            tekniskTid = LocalDateTime.now(),
            behandlingType = "KLAGE",
            sakYtelse = SakYtelseDvh.fraStønadstype(fagsak.stønadstype),
            relatertEksternBehandlingId = påklagetVedtakDetaljer?.eksternFagsystemBehandlingId,
            relatertFagsystemType = påklagetVedtakDetaljer?.fagsystemType?.name,
            behandlingStatus = hendelse.name,
            opprettetAv = maskerVerdiHvisStrengtFortrolig(erStrengtFortrolig, behandling.sporbar.opprettetAv),
            ferdigBehandletTid = ferdigBehandletTid(hendelse, hendelseTidspunkt),
            ansvarligEnhet = behandlendeEnhet,
            mottattTid = behandling.klageMottatt.atStartOfDay(),
            sakUtland =
                behandling.påklagetVedtak.påklagetVedtakDetaljer
                    ?.regelverk
                    .tilDVHSakNasjonalitet(),
            behandlingResultat = behandlingResultat(hendelse, behandling),
            resultatBegrunnelse = resultatBegrunnelse(behandling, vurdering),
            behandlingMetode = "MANUELL",
            kravMottatt = behandling.klageMottatt,
            saksbehandler =
                maskerVerdiHvisStrengtFortrolig(
                    erStrengtFortrolig,
                    gjeldendeSaksbehandler ?: behandling.sporbar.endret.endretAv,
                ),
            avsender = "Tilleggsstonader Klage",
            versjon = null,
        )
    }

    private fun resultatBegrunnelse(
        behandling: Behandling,
        vurdering: Vurdering?,
    ) = if (behandling.resultat == BehandlingResultat.HENLAGT) {
        behandling.henlagtÅrsak?.name
    } else {
        vurdering?.årsak?.name
    }

    private fun behandlingResultat(
        hendelse: BehandlingsstatistikkHendelse,
        behandling: Behandling,
    ) = if (hendelse == FERDIG || hendelse == SENDT_TIL_KA) {
        behandling.resultat.name
    } else {
        null
    }

    private fun ferdigBehandletTid(
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime,
    ) = if (hendelse == FERDIG || hendelse == SENDT_TIL_KA) {
        hendelseTidspunkt
    } else {
        null
    }

    private fun maskerVerdiHvisStrengtFortrolig(
        erStrengtFortrolig: Boolean,
        verdi: String,
    ): String {
        if (erStrengtFortrolig) {
            return "-5"
        }
        return verdi
    }

    private fun Regelverk?.tilDVHSakNasjonalitet(): String? =
        when (this) {
            Regelverk.NASJONAL -> "Nasjonal"
            Regelverk.EØS -> "Utland"
            null -> "Nasjonal" // fallback til Nasjonal der vedtaket ikkje har informasjon om det er nasjonal eller EØS
        }
}
