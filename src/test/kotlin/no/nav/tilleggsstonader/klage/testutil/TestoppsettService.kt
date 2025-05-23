package no.nav.tilleggsstonader.klage.testutil

import no.nav.tilleggsstonader.klage.behandling.BehandlingRepository
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.fagsak.FagsakPersonRepository
import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository
            .insert(
                FagsakDomain(
                    id = fagsak.id,
                    fagsakPersonId = person.id,
                    stønadstype = fagsak.stønadstype,
                    fagsystem = fagsak.fagsystem,
                    eksternId = fagsak.eksternId,
                    sporbar = fagsak.sporbar,
                ),
            ).tilFagsakMedPerson(person.identer)
    }

    fun hentBehandling(behandlingId: BehandlingId) = behandlingRepository.findByIdOrThrow(behandlingId)

    fun lagreBehandling(behandling: Behandling): Behandling = behandlingRepository.insert(behandling)

    fun lagreBehandlingMedFagsak(behandling: Behandling): Behandling {
        lagreFagsak(fagsak(id = behandling.fagsakId))
        return behandlingRepository.insert(behandling)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson {
        val person = fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
        return person ?: fagsakPersonRepository.insert(
            FagsakPerson(
                fagsak.fagsakPersonId,
                identer = fagsak.personIdenter,
            ),
        )
    }
}
