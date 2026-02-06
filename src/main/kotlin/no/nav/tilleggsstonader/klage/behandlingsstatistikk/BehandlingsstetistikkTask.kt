package no.nav.tilleggsstonader.klage.behandlingsstatistikk

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til iverksett",
    maxAntallFeil = 4,
    settTilManuellOppfølgning = true,
)
class BehandlingsstatistikkTask(
    private val behandlingStatistikkService: BehandlingsstatistikkService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler) =
            jsonMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)
        behandlingStatistikkService.sendBehandlingstatistikk(
            behandlingId,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler,
        )
    }

    companion object {
        fun opprettMottattTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.MOTTATT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
            )

        fun opprettPåbegyntTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
            )

        fun opprettVenterTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.VENTER,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
            )

        fun opprettFerdigTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
            )

        fun opprettSendtTilKATask(
            behandlingId: BehandlingId,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String = SikkerhetContext.hentSaksbehandler(true),
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.SENDT_TIL_KA,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = gjeldendeSaksbehandler,
            )

        private fun opprettTask(
            behandlingId: BehandlingId,
            hendelse: BehandlingsstatistikkHendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        BehandlingsstatistikkTaskPayload(
                            behandlingId,
                            hendelse,
                            hendelseTidspunkt,
                            gjeldendeSaksbehandler,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = gjeldendeSaksbehandler
                        this["behandlingId"] = behandlingId.toString()
                        this["hendelse"] = hendelse.name
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                    },
            )

        const val TYPE = "behandlingsstatistikkKlageTask"
    }
}

data class BehandlingsstatistikkTaskPayload(
    val behandlingId: BehandlingId,
    val hendelse: BehandlingsstatistikkHendelse,
    val hendelseTidspunkt: LocalDateTime,
    val gjeldendeSaksbehandler: String?,
)
