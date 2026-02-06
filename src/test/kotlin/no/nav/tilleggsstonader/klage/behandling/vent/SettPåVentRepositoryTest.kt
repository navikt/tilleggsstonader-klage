package no.nav.tilleggsstonader.klage.behandling.vent

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SettPåVentRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var repository: SettPåVentRepository

    @Test
    fun `skal kunne lagre og hente settPåVent`() {
        val behandling = testoppsettService.lagreBehandlingMedFagsak(behandling())

        repository.insert(settPåVent(behandling, aktiv = false))
        val aktiv = repository.insert(settPåVent(behandling, aktiv = true))
        repository.insert(settPåVent(behandling, aktiv = false))

        val settPåVent = repository.findByBehandlingIdAndAktivIsTrue(behandling.id)!!

        assertThat(settPåVent.id).isEqualTo(aktiv.id)
        assertThat(settPåVent.behandlingId).isEqualTo(aktiv.behandlingId)
        assertThat(settPåVent.oppgaveId).isEqualTo(aktiv.oppgaveId)
        assertThat(settPåVent.årsaker).isEqualTo(aktiv.årsaker)
        assertThat(settPåVent.kommentar).isEqualTo(aktiv.kommentar)
        assertThat(settPåVent.aktiv).isEqualTo(aktiv.aktiv)
        assertThat(settPåVent.taAvVentKommentar).isEqualTo(aktiv.taAvVentKommentar)
        assertThat(settPåVent.sporbar).isEqualTo(aktiv.sporbar)

        assertThat(settPåVent).isEqualTo(aktiv)
    }

    @Test
    fun `skal ikke kunne ha 2 aktive på samme behandling`() {
        val behandling = testoppsettService.lagreBehandlingMedFagsak(behandling())

        repository.insert(settPåVent(behandling))

        val exception =
            catchException {
                repository.insert(settPåVent(behandling))
            }
        assertThat(exception.cause).hasMessageContaining("duplicate key value violates unique constraint")
    }

    fun settPåVent(
        behandling: Behandling,
        aktiv: Boolean = true,
    ) = SettPåVent(
        behandlingId = behandling.id,
        årsaker = listOf(ÅrsakSettPåVent.ANNET),
        oppgaveId = 1,
        aktiv = aktiv,
        kommentar = "kommentar",
    )
}
