package no.nav.tilleggsstonader.klage.oppgave

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
    private val oppgaveClient: OppgaveClient,
    private val behandlingService: BehandlingService,
) {

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        return oppgaveClient.opprettOppgave(opprettOppgaveRequest)
    }

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        // Skal ikke oppdatere tema for oppgaver som alt er ferdigstilt
        if (!behandling.status.erUnderArbeidAvSaksbehandler()) return

        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        // TODO: Bør sende med oppgaveId til EksternBehandlingContoller og deretter slette dette kallet
        val oppgave = hentOppgave(behandleSakOppgave.oppgaveId)

        val oppdatertOppgave = Oppgave(
            id = behandleSakOppgave.oppgaveId,
            behandlingstema = Behandlingstema.Tilbakebetaling.value,
            versjon = oppgave.versjon,
        )

        oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }

    fun hentOppgave(gsakOppgaveId: Long): Oppgave {
        return oppgaveClient.finnOppgaveMedId(gsakOppgaveId)
    }
}
