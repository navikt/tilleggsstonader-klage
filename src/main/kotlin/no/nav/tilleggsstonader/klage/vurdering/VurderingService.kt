package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.behandling.StegService
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.brev.BrevRepository
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemler
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto
import no.nav.tilleggsstonader.klage.vurdering.dto.tilDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VurderingService(
    private val vurderingRepository: VurderingRepository,
    private val stegService: StegService,
    private val brevRepository: BrevRepository,
) {
    fun hentVurdering(behandlingId: BehandlingId): Vurdering? = vurderingRepository.findByIdOrNull(behandlingId)

    fun hentVurderingDto(behandlingId: BehandlingId): VurderingDto? = hentVurdering(behandlingId)?.tilDto()

    @Transactional
    fun opprettEllerOppdaterVurdering(vurdering: VurderingDto): VurderingDto {
        validerVurdering(vurdering)
        if (vurdering.vedtak === Vedtak.OMGJØR_VEDTAK) {
            brevRepository.deleteById(vurdering.behandlingId)
        }

        stegService.oppdaterSteg(vurdering.behandlingId, StegType.VURDERING, StegType.BREV)

        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDto()
        } else {
            opprettNyVurdering(vurdering).tilDto()
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
                hjemler = vurdering.hjemler?.let { Hjemler(it) },
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
                hjemler = vurdering.hjemler?.let { Hjemler(it) },
                interntNotat = vurdering.interntNotat,
            ),
        )
}
