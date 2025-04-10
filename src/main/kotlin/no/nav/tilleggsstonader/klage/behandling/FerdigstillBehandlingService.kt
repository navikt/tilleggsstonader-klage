package no.nav.tilleggsstonader.klage.behandling

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.OpprettRevurderingUtil.skalOppretteRevurderingAutomatisk
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.tilleggsstonader.klage.behandling.domain.tilFagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.tilleggsstonader.klage.brev.BrevService
import no.nav.tilleggsstonader.klage.distribusjon.JournalførBrevTask
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.oppgave.OppgaveTaskService
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat.HENLAGT
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat.IKKE_MEDHOLD
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat.IKKE_SATT
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat.MEDHOLD
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties

@Service
class FerdigstillBehandlingService(
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val formService: FormService,
    private val stegService: StegService,
    private val taskService: TaskService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val brevService: BrevService,
    private val fagsystemVedtakService: FagsystemVedtakService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {
    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    @Transactional
    fun ferdigstillKlagebehandling(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingsresultat = utledBehandlingResultat(behandlingId)

        validerKanFerdigstille(behandling)
        if (behandlingsresultat != MEDHOLD) {
            brevService.lagBrevPdf(behandlingId)
            opprettJournalførBrevTask(behandlingId)
        }
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id)

        val opprettetRevurdering = opprettRevurderingHvisMedhold(behandling, behandlingsresultat)

        behandlingService.oppdaterBehandlingMedResultat(behandlingId, behandlingsresultat, opprettetRevurdering)
        stegService.oppdaterSteg(behandlingId, behandling.steg, stegForResultat(behandlingsresultat), behandlingsresultat)
        taskService.save(LagSaksbehandlingsblankettTask.opprettTask(behandlingId))

        if (behandlingsresultat == IKKE_MEDHOLD) {
            taskService.save(BehandlingsstatistikkTask.opprettSendtTilKATask(behandlingId = behandlingId))
        }

        behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandlingId)

        taskService.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = behandlingId))
    }

    /**
     * Oppretter revurdering automatisk ved medhold
     * Dette skjer synkront og kan vurderes å endres til async med task eller kafka ved behov
     */
    private fun opprettRevurderingHvisMedhold(
        behandling: Behandling,
        behandlingsresultat: BehandlingResultat,
    ): FagsystemRevurdering? =
        if (behandlingsresultat == MEDHOLD &&
            skalOppretteRevurderingAutomatisk(behandling.påklagetVedtak)
        ) {
            fagsystemVedtakService.opprettRevurdering(behandling.id).tilFagsystemRevurdering()
        } else {
            null
        }

    private fun opprettJournalførBrevTask(behandlingId: BehandlingId) {
        val journalførBrevTask =
            Task(
                type = JournalførBrevTask.TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                    },
            )
        taskService.save(journalførBrevTask)
    }

    private fun stegForResultat(resultat: BehandlingResultat): StegType =
        when (resultat) {
            IKKE_MEDHOLD -> StegType.KABAL_VENTER_SVAR
            MEDHOLD, IKKE_MEDHOLD_FORMKRAV_AVVIST, HENLAGT -> StegType.BEHANDLING_FERDIGSTILT
            IKKE_SATT -> error("Kan ikke utlede neste steg når behandlingsresultatet er IKKE_SATT")
        }

    private fun validerKanFerdigstille(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Kan ikke ferdigstille behandlingen da den er låst for videre behandling")
        }
        if (behandling.steg != StegType.BREV) {
            throw Feil("Kan ikke ferdigstille behandlingen fra steg=${behandling.steg}")
        }
    }

    private fun utledBehandlingResultat(behandlingId: BehandlingId): BehandlingResultat =
        if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            vurderingService.hentVurdering(behandlingId)?.vedtak?.tilBehandlingResultat()
                ?: throw Feil("Burde funnet behandling $behandlingId")
        } else {
            IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
}
