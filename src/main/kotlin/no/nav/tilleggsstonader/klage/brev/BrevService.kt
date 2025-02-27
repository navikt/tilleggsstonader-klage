package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.tilleggsstonader.klage.brev.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.tilleggsstonader.klage.brev.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.Brev
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakerPerson
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.brev.domain.MottakerRolle
import no.nav.tilleggsstonader.klage.brev.dto.BrevmottakereDto
import no.nav.tilleggsstonader.klage.brev.dto.FritekstBrevRequestDto
import no.nav.tilleggsstonader.klage.brev.dto.tilDomene
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Fil
import no.nav.tilleggsstonader.klage.formkrav.FormService
import no.nav.tilleggsstonader.klage.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.personopplysninger.PersonopplysningerService
import no.nav.tilleggsstonader.klage.vurdering.VurderingService
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BrevService(
    private val htmlifyClient: HtmlifyClient,
    private val brevRepository: BrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun hentBrev(behandlingId: BehandlingId): Brev = brevRepository.findByIdOrThrow(behandlingId)

    fun hentBrevmottakere(behandlingId: BehandlingId): Brevmottakere {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        return brev.mottakere ?: Brevmottakere()
    }

    fun settBrevmottakere(
        behandlingId: BehandlingId,
        brevmottakere: BrevmottakereDto,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerKanLageBrev(behandling)

        val mottakere = brevmottakere.tilDomene()

        validerUnikeBrevmottakere(mottakere)
        validerMinimumEnMottaker(mottakere)

        val brev = brevRepository.findByIdOrThrow(behandlingId)
        brevRepository.update(brev.copy(mottakere = mottakere))
    }

    fun lagBrev(behandlingId: BehandlingId): ByteArray {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val navn = personopplysninger.navn
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer
        validerKanLageBrev(behandling)

        val brevRequest = lagBrevRequest(behandling, fagsak, navn, påklagetVedtakDetaljer, behandling.klageMottatt)

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysninger)

        val html =
            htmlifyClient.genererHtmlFritekstbrev(
                fritekstBrev = brevRequest,
                saksbehandlerNavn = signaturMedEnhet.navn,
                enhet = signaturMedEnhet.enhet,
            )

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html,
            fagsak = fagsak,
        )

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun validerKanLageBrev(behandling: Behandling) {
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere brev når behandlingen er låst"
        }
        feilHvis(behandling.steg != StegType.BREV) {
            "Behandlingen er i feil steg (${behandling.steg}) steg=${StegType.BREV} for å kunne oppdatere brevet"
        }
    }

    private fun lagBrevRequest(
        behandling: Behandling,
        fagsak: Fagsak,
        navn: String,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto {
        val behandlingResultat = utledBehandlingResultat(behandling.id)
        val vurdering = vurderingService.hentVurdering(behandling.id)

        return when (behandlingResultat) {
            BehandlingResultat.IKKE_MEDHOLD -> {
                val instillingKlageinstans =
                    vurdering?.innstillingKlageinstans
                        ?: throw Feil("Behandling med resultat $behandlingResultat mangler instillingKlageinstans for generering av brev")
                brukerfeilHvis(påklagetVedtakDetaljer == null) {
                    "Kan ikke opprette brev til klageinstansen når det ikke er valgt et påklaget vedtak"
                }
                BrevInnhold.lagOpprettholdelseBrev(
                    ident = fagsak.hentAktivIdent(),
                    instillingKlageinstans = instillingKlageinstans,
                    navn = navn,
                    stønadstype = fagsak.stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )
            }
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST -> {
                val formkrav = formService.hentForm(behandling.id)
                return when (behandling.påklagetVedtak.påklagetVedtakstype) {
                    PåklagetVedtakstype.UTEN_VEDTAK ->
                        BrevInnhold.lagFormkravAvvistBrevIkkePåklagetVedtak(
                            ident = fagsak.hentAktivIdent(),
                            navn = navn,
                            formkrav = formkrav,
                            stønadstype = fagsak.stønadstype,
                        )
                    else ->
                        BrevInnhold.lagFormkravAvvistBrev(
                            ident = fagsak.hentAktivIdent(),
                            navn = navn,
                            formkrav = formkrav,
                            stønadstype = fagsak.stønadstype,
                        )
                }
            }
            BehandlingResultat.MEDHOLD,
            BehandlingResultat.IKKE_SATT,
            BehandlingResultat.HENLAGT,
            -> throw Feil("Kan ikke lage brev for behandling med behandlingResultat=$behandlingResultat")
        }
    }

    fun hentBrevPdf(behandlingId: BehandlingId): ByteArray =
        brevRepository.findByIdOrThrow(behandlingId).pdf?.bytes
            ?: error("Finner ikke brev-pdf for behandling=$behandlingId")

    private fun lagreEllerOppdaterBrev(
        behandlingId: BehandlingId,
        saksbehandlerHtml: String,
        fagsak: Fagsak,
    ): Brev {
        val brev = brevRepository.findByIdOrNull(behandlingId)
        return if (brev != null) {
            brevRepository.update(brev.copy(saksbehandlerHtml = saksbehandlerHtml))
        } else {
            brevRepository.insert(
                Brev(
                    behandlingId = behandlingId,
                    saksbehandlerHtml = saksbehandlerHtml,
                    mottakere = initialiserBrevmottakere(behandlingId, fagsak),
                ),
            )
        }
    }

    private fun initialiserBrevmottakere(
        behandlingId: BehandlingId,
        fagsak: Fagsak,
    ) = Brevmottakere(
        personer =
            listOf(
                BrevmottakerPerson(
                    personIdent = fagsak.hentAktivIdent(),
                    navn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
                    mottakerRolle = MottakerRolle.BRUKER,
                ),
            ),
    )

    fun lagBrevPdf(behandlingId: BehandlingId) {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        brevRepository.update(brev.copy(pdf = Fil(generertBrev)))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterMottakerJournalpost(
        behandlingId: BehandlingId,
        brevmottakereJournalposter: BrevmottakereJournalposter,
    ) {
        brevRepository.oppdaterMottakerJournalpost(behandlingId, brevmottakereJournalposter)
    }

    private fun utledBehandlingResultat(behandlingId: BehandlingId): BehandlingResultat =
        if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            vurderingService.hentVurdering(behandlingId)?.vedtak?.tilBehandlingResultat()
                ?: throw Feil("Burde funnet behandling $behandlingId")
        } else {
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
}
