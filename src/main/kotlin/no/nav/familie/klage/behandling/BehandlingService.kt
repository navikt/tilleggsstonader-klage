package no.nav.tilleggsstonader.klage.behandling

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType.BEHANDLING_FERDIGSTILT
import no.nav.tilleggsstonader.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.tilleggsstonader.klage.behandling.domain.harManuellVedtaksdato
import no.nav.tilleggsstonader.klage.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.klage.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.behandling.dto.tilDto
import no.nav.tilleggsstonader.klage.behandling.dto.tilPåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.domain.SporbarUtils
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.klage.integrasjoner.FagsystemVedtakService
import no.nav.tilleggsstonader.klage.kabal.KlageresultatRepository
import no.nav.tilleggsstonader.klage.kabal.domain.tilDto
import no.nav.tilleggsstonader.klage.oppgave.OppgaveTaskService
import no.nav.tilleggsstonader.klage.repository.findByIdOrThrow
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansResultatDto
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus.FERDIGSTILT
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val klageresultatRepository: KlageresultatRepository,
    private val behandlinghistorikkService: BehandlingshistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val taskService: TaskService,
    private val fagsystemVedtakService: FagsystemVedtakService,
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return behandlingRepository.findByIdOrThrow(behandlingId)
            .tilDto(fagsak, hentKlageresultatDto(behandlingId))
    }

    fun opprettBehandling(behandling: Behandling): Behandling {
        return behandlingRepository.insert(behandling)
    }

    fun hentKlageresultatDto(behandlingId: UUID): List<KlageinstansResultatDto> {
        val klageresultater = klageresultatRepository.findByBehandlingId(behandlingId)
        return klageresultater.tilDto()
    }

    fun finnKlagebehandlingsresultat(eksternFagsakId: String, fagsystem: Fagsystem): List<Klagebehandlingsresultat> {
        return behandlingRepository.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem)
    }

    fun hentAktivIdent(behandlingId: UUID): Pair<String, Fagsak> {
        val behandling = hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        return Pair(fagsak.hentAktivIdent(), fagsak)
    }

    fun oppdaterBehandlingMedResultat(
        behandlingId: UUID,
        behandlingsresultat: BehandlingResultat,
        opprettetRevurdering: FagsystemRevurdering?,
    ) {
        val behandling = hentBehandling(behandlingId)
        if (behandling.resultat != BehandlingResultat.IKKE_SATT) {
            error("Kan ikke endre på et resultat som allerede er satt")
        }
        val oppdatertBehandling = behandling.copy(
            resultat = behandlingsresultat,
            vedtakDato = LocalDateTime.now(),
            fagsystemRevurdering = opprettetRevurdering,
        )
        behandlingRepository.update(oppdatertBehandling)
    }

    @Transactional
    fun oppdaterPåklagetVedtak(behandlingId: UUID, påklagetVedtakDto: PåklagetVedtakDto) {
        val behandling = hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere påklaget vedtak siden behandlingen er låst for videre saksbehandling"
        }
        feilHvisIkke(påklagetVedtakDto.erGyldig()) {
            "Påklaget vedtak er i en ugyldig tilstand: EksternFagsystemBehandlingId:${påklagetVedtakDto.eksternFagsystemBehandlingId}, PåklagetVedtakType: ${påklagetVedtakDto.påklagetVedtakstype}"
        }

        feilHvis(påklagetVedtakDto.manglerVedtaksDato()) {
            "Må fylle inn vedtaksdato når valgt vedtakstype er ${påklagetVedtakDto.påklagetVedtakstype}"
        }

        val påklagetVedtakDetaljer = påklagetVedtakDetaljer(behandlingId, påklagetVedtakDto)

        val behandlingMedPåklagetVedtak = behandling.copy(
            påklagetVedtak = PåklagetVedtak(
                påklagetVedtakstype = påklagetVedtakDto.påklagetVedtakstype,
                påklagetVedtakDetaljer = påklagetVedtakDetaljer,
            ),
        )
        behandlingRepository.update(behandlingMedPåklagetVedtak)
    }

    private fun påklagetVedtakDetaljer(
        behandlingId: UUID,
        påklagetVedtakDto: PåklagetVedtakDto,
    ): PåklagetVedtakDetaljer? {
        if (påklagetVedtakDto.påklagetVedtakstype.harManuellVedtaksdato()) {
            return tilPåklagetVedtakDetaljerMedManuellDato(påklagetVedtakDto)
        }
        return påklagetVedtakDto.eksternFagsystemBehandlingId?.let {
            fagsystemVedtakService.hentFagsystemVedtakForPåklagetBehandlingId(behandlingId, it)
                .tilPåklagetVedtakDetaljer()
        }
    }

    private fun tilPåklagetVedtakDetaljerMedManuellDato(påklagetVedtakDto: PåklagetVedtakDto) =
        PåklagetVedtakDetaljer(
            fagsystemType = utledFagsystemType(påklagetVedtakDto),
            eksternFagsystemBehandlingId = null,
            behandlingstype = "",
            resultat = "",
            vedtakstidspunkt = påklagetVedtakDto.manuellVedtaksdato?.atStartOfDay() ?: error("Mangler vedtaksdato"),
            regelverk = påklagetVedtakDto.regelverk,
        )

    private fun utledFagsystemType(påklagetVedtakDto: PåklagetVedtakDto): FagsystemType {
        return when (påklagetVedtakDto.påklagetVedtakstype) {
            PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING -> FagsystemType.TILBAKEKREVING
            PåklagetVedtakstype.UTESTENGELSE -> FagsystemType.UTESTENGELSE
            PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK -> FagsystemType.ORDNIÆR
            else -> error("Kan ikke utlede fagsystemType for påklagetVedtakType ${påklagetVedtakDto.påklagetVedtakstype}")
        }
    }

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto) {
        val behandling = hentBehandling(behandlingId)

        validerKanHenleggeBehandling(behandling)

        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
            resultat = BehandlingResultat.HENLAGT,
            steg = BEHANDLING_FERDIGSTILT,
            status = FERDIGSTILT,
            vedtakDato = SporbarUtils.now(),
        )

        behandlinghistorikkService.opprettBehandlingshistorikk(behandlingId, BEHANDLING_FERDIGSTILT)
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id)
        behandlingRepository.update(henlagtBehandling)
        taskService.save(taskService.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = behandlingId)))
    }

    private fun validerKanHenleggeBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke henlegge behandling med status ${behandling.status}"
        }
    }
}
