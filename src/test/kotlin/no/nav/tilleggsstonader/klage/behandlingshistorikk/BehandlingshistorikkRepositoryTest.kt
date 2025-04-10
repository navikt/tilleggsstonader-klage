package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.klage.infrastruktur.repository.JsonWrapper
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingshistorikkRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var repository: BehandlingshistorikkRepository

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre og hente opp`() {
        val behandlingshistorikk =
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = StegType.OPPRETTET,
                utfall = StegUtfall.SATT_PÅ_VENT,
                metadata = JsonWrapper(objectMapper.writeValueAsString(mapOf("key" to "value"))),
                opprettetAvNavn = "OpprettetAvNavn",
                opprettetAv = "Navn",
                gitVersjon = "abc",
            )
        repository.insert(behandlingshistorikk)

        val fraDb = repository.findByIdOrThrow(behandlingshistorikk.id)

        assertThat(fraDb)
            .usingRecursiveComparison()
            .ignoringFields("metadata")
            .isEqualTo(behandlingshistorikk)
        assertThat(fraDb.metadata!!.json).isEqualToIgnoringWhitespace(behandlingshistorikk.metadata!!.json)
    }

    @Test
    fun `skal kunne lagre og hente opp verdier med nullverdier`() {
        val behandlingshistorikk =
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = StegType.OPPRETTET,
                utfall = null,
                metadata = null,
                opprettetAvNavn = null,
                opprettetAv = "Navn",
                gitVersjon = null,
            )
        repository.insert(behandlingshistorikk)

        val fraDb = repository.findByIdOrThrow(behandlingshistorikk.id)
        assertThat(fraDb).isEqualTo(behandlingshistorikk)
    }
}
