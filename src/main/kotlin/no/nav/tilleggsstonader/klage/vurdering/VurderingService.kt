package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.brev.BrevRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemler
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto
import no.nav.tilleggsstonader.klage.vurdering.dto.tilDomene
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VurderingService(
    private val vurderingRepository: VurderingRepository,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val stegService: StegService,
    private val brevRepository: BrevRepository,
) {
    fun hentVurdering(behandlingId: BehandlingId): Vurdering? = vurderingRepository.findByIdOrNull(behandlingId)

    fun hentVurderingDto(behandlingId: BehandlingId): VurderingDto? = hentVurdering(behandlingId)?.tilDomene()

    @Transactional
    fun opprettEllerOppdaterVurdering(vurdering: VurderingDto): VurderingDto {
        val behandling = behandlingService.hentBehandling(vurdering.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        validerVurdering(vurdering, fagsak.stønadstype)

        if (vurdering.vedtak === Vedtak.OMGJØR_VEDTAK) {
            brevRepository.deleteById(vurdering.behandlingId)
        }

        stegService.oppdaterSteg(vurdering.behandlingId, StegType.VURDERING, StegType.BREV)

        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDomene()
        } else {
            opprettNyVurdering(vurdering).tilDomene()
        }
    }

    fun slettVurderingForBehandling(behandlingId: BehandlingId) {
        vurderingRepository.deleteById(behandlingId)
    }

    private fun opprettNyVurdering(vurdering: VurderingDto) =
        vurderingRepository.insert(
            Vurdering(
                behandlingId = vurdering.behandlingId,
                vedtak = vurdering.vedtak,
                årsak = vurdering.årsak,
                begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
                hjemler = vurdering.hjemler?.let { Hjemler(it.tilDomene()) },
                innstillingKlageinstans = vurdering.innstillingKlageinstans,
                interntNotat = vurdering.interntNotat,
            ),
        )

    private fun oppdaterVurdering(
        vurdering: VurderingDto,
        eksisterendeVurdering: Vurdering,
    ): Vurdering =
        vurderingRepository.update(
            eksisterendeVurdering.copy(
                vedtak = vurdering.vedtak,
                innstillingKlageinstans = vurdering.innstillingKlageinstans,
                årsak = vurdering.årsak,
                begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
                hjemler = vurdering.hjemler?.let { Hjemler(it.tilDomene()) },
                interntNotat = vurdering.interntNotat,
            ),
        )
}
