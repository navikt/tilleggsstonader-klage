package no.nav.tilleggsstonader.klage.formkrav

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.dto.tilDto
import no.nav.tilleggsstonader.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.formkrav.FormUtil.alleVilkårOppfylt
import no.nav.tilleggsstonader.klage.formkrav.FormUtil.utledFormresultat
import no.nav.tilleggsstonader.klage.formkrav.domain.Form
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.dto.FormkravDto
import no.nav.tilleggsstonader.klage.formkrav.dto.tilDto
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FormService(
    private val formRepository: FormRepository,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val vurderingService: VurderingService,
    private val taskService: TaskService,
) {
    fun hentForm(behandlingId: BehandlingId): Form = formRepository.findByIdOrThrow(behandlingId)

    @Transactional
    fun opprettInitielleFormkrav(behandlingId: BehandlingId): Form = formRepository.insert(Form(behandlingId = behandlingId))

    @Transactional
    fun oppdaterFormkrav(formkrav: FormkravDto): FormkravDto {
        val behandlingId = formkrav.behandlingId
        val nyttPåklagetVedtak = formkrav.påklagetVedtak

        val oppdaterteFormkrav =
            formRepository.findByIdOrThrow(behandlingId).copy(
                klagePart = formkrav.klagePart,
                klagefristOverholdt = formkrav.klagefristOverholdt,
                klagefristOverholdtUnntak = formkrav.klagefristOverholdtUnntak,
                klageKonkret = formkrav.klageKonkret,
                klageSignert = formkrav.klageSignert,
                saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
                brevtekst = formkrav.brevtekst,
            )
        behandlingService.oppdaterPåklagetVedtak(behandlingId, nyttPåklagetVedtak)

        val formresultat = utledFormresultat(oppdaterteFormkrav, nyttPåklagetVedtak)
        when (formresultat) {
            FormVilkår.OPPFYLT -> {
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.VURDERING)
            }
            FormVilkår.IKKE_OPPFYLT -> {
                vurderingService.slettVurderingForBehandling(behandlingId)
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.BREV)
            }
            FormVilkår.IKKE_SATT -> {
                vurderingService.slettVurderingForBehandling(behandlingId)
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.FORMKRAV)
            }
        }

        opprettBehandlingsstatistikk(behandlingId)

        return formRepository.update(oppdaterteFormkrav).tilDto(nyttPåklagetVedtak)
    }

    private fun opprettBehandlingsstatistikk(behandlingId: BehandlingId) {
        behandlingshistorikkService.hentBehandlingshistorikk(behandlingId).find { it.steg == StegType.FORMKRAV }
            ?: run {
                taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = behandlingId))
            }
    }

    fun formkravErOppfyltForBehandling(behandlingId: BehandlingId): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return alleVilkårOppfylt(form)
    }

    fun hentFormDto(behandlingId: BehandlingId): FormkravDto {
        val påklagetVedtak = behandlingService.hentBehandling(behandlingId).påklagetVedtak
        return hentForm(behandlingId).tilDto(påklagetVedtak.tilDto())
    }
}
