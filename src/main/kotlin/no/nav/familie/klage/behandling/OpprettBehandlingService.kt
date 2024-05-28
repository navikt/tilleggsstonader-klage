package no.nav.tilleggsstonader.klage.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.oppgave.OppgaveTaskService
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val formService: FormService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettBehandling(
        opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest,
    ): UUID {
        val klageMottatt = opprettKlagebehandlingRequest.klageMottatt
        val stønadstype = opprettKlagebehandlingRequest.stønadstype
        val eksternFagsakId = opprettKlagebehandlingRequest.eksternFagsakId

        feilHvis(klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for eksternFagsakId=$eksternFagsakId"
        }

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            ident = opprettKlagebehandlingRequest.ident,
            eksternId = eksternFagsakId,
            fagsystem = opprettKlagebehandlingRequest.fagsystem,
            stønadstype = stønadstype,
        )

        val behandlingId = behandlingService.opprettBehandling(
            Behandling(
                fagsakId = fagsak.id,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT,
                ),
                klageMottatt = klageMottatt,
                behandlendeEnhet = opprettKlagebehandlingRequest.behandlendeEnhet,
            ),
        ).id

        behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.OPPRETTET)

        formService.opprettInitielleFormkrav(behandlingId)

        oppgaveTaskService.opprettBehandleSakOppgave(behandlingId, opprettKlagebehandlingRequest.klageGjelderTilbakekreving)
        // TODO: Utkommenter denne etter at BehandlingsstatistikkTask er re-implementert
//        taskService.save(
//            BehandlingsstatistikkTask.opprettMottattTask(behandlingId = behandlingId),
//        )
        logger.info(
            "Opprettet behandling=$behandlingId for stønadstype=$stønadstype " +
                "eksternFagsakId=$eksternFagsakId klageMottatt=$klageMottatt",
        )
        return behandlingId
    }
}
