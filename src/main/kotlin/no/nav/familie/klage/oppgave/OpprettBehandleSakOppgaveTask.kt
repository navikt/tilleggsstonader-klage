package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.klageGjelderTilbakekrevingMetadataKey
import no.nav.tilleggsstonader.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.tilleggsstonader.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.prosessering.AsyncTaskStep
import no.nav.tilleggsstonader.prosessering.TaskStepBeskrivelse
import no.nav.tilleggsstonader.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettBehandleSakOppgaveTask.TYPE,
    beskrivelse = "Opprett behandle sak oppgave",
)
class OpprettBehandleSakOppgaveTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveClient: OppgaveClient,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val klageGjelderTilbakekreving: Boolean = task.metadata.getProperty(klageGjelderTilbakekrevingMetadataKey).toBoolean()

        val oppgaveRequest = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = fagsak.hentAktivIdent(), gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            saksId = fagsak.eksternId, // fagsakId fra fagsystem
            tema = fagsak.stønadstype.tilTema(), //
            oppgavetype = Oppgavetype.BehandleSak,
            fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = "Klagebehandling i ny løsning",
            enhetsnummer = behandling.behandlendeEnhet,
            behandlingstype = Behandlingstema.Klage.value,
            behandlesAvApplikasjon = "familie-klage",
            tilordnetRessurs = task.metadata.getProperty(saksbehandlerMetadataKey),
            behandlingstema = if (klageGjelderTilbakekreving) Behandlingstema.Tilbakebetaling.value else null,
        )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest = oppgaveRequest)
        behandleSakOppgaveRepository.insert(
            BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = oppgaveId),
        )
    }

    companion object {
        const val TYPE = "opprettBehandleSakoppgave"
    }
}
