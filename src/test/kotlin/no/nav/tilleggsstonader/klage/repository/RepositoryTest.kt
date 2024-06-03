package no.nav.tilleggsstonader.klage.repository

import no.nav.tilleggsstonader.klage.fagsak.FagsakRepository
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.klage.felles.domain.Endret
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class RepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal oppdatere sporbar automatisk når en entitet oppdateres`() {
        val opprinneligEndret = Endret(endretAv = "~", endretTid = LocalDateTime.MIN)
        val person = testoppsettService.opprettPerson(FagsakPerson(identer = setOf()))
        val fagsak = fagsakRepository.insert(fagsakDomain(personId = person.id))
        val opprettetSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar
        val opprettetTid = fagsak.sporbar.opprettetTid
        val opprettetAv = "VL"

        // verifiser att opprettetAv og opprettetTid ikke er mulig å sette
        // kanskje ikke ønskelig men nå oppfattes alle våre entiteter som nye då vi har satt ID på de
        assertThat(opprettetSporbar.endret.endretAv).isNotEqualTo(opprinneligEndret.endretAv)
        assertThat(opprettetSporbar.endret.endretTid).isNotEqualTo(opprinneligEndret.endretTid)

        assertThat(opprettetSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(opprettetSporbar.endret.endretAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.endret.endretTid).isEqualTo(fagsak.sporbar.endret.endretTid)

        val nyEndretAv = "En annen brukere"
        BrukerContextUtil.mockBrukerContext(nyEndretAv)
        val oppdatertFagsak = fagsakRepository.update(fagsak)
        val oppdatertSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar

        assertThat(oppdatertSporbar.endret.endretTid).isNotEqualTo(opprettetSporbar.endret.endretTid)
        assertThat(oppdatertSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(oppdatertSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(oppdatertSporbar.endret.endretAv).isEqualTo(nyEndretAv)
        assertThat(oppdatertSporbar.endret.endretTid).isEqualTo(oppdatertFagsak.sporbar.endret.endretTid)
    }
}
