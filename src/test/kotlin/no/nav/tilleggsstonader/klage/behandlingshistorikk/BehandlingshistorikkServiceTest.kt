package no.nav.tilleggsstonader.klage.behandlingshistorikk

import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.klage.behandling.vent.SettPåVentService
import no.nav.tilleggsstonader.klage.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.klage.behandlingshistorikk.domain.StegUtfall
import no.nav.tilleggsstonader.klage.infrastruktur.repository.tilJson
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingshistorikkServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    lateinit var settPåVentService: SettPåVentService

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreBehandlingMedFagsak(behandling)
    }

    @Nested
    inner class PåVent {
        @Test
        fun `skal lagre kommentaren i metadata når man setter behandling på vent`() {
            sattPåVent()
            with(behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).single()) {
                assertThat(this.utfall).isEqualTo(StegUtfall.SATT_PÅ_VENT)
                val forventetMetadata =
                    mapOf(
                        "kommentarSettPåVent" to "en kommentar",
                        "årsaker" to listOf(ÅrsakSettPåVent.REGISTRERING_AV_TILTAK.name),
                    )
                assertThat(metadata.tilJson()!!).isEqualTo(forventetMetadata)
            }
        }

        @Test
        fun `skal lagre kommentaren i metadata når man tar en behandling av vent`() {
            taAvVent()
            with(behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).single()) {
                assertThat(this.utfall).isEqualTo(StegUtfall.TATT_AV_VENT)
                val forventetMetadata =
                    mapOf(
                        "kommentar" to "tatt av vent",
                    )
                assertThat(metadata.tilJson()!!).isEqualTo(forventetMetadata)
            }
        }
    }

    @Nested
    inner class SlettFritekstMetadataVedFerdigstillelse {
        @Test
        fun `skal slette kommentaren i metadata fra på-vent-hendelse`() {
            sattPåVent()
            behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandling.id)
            with(behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).single()) {
                assertThat(this.utfall).isEqualTo(StegUtfall.SATT_PÅ_VENT)
                val forventetMetadata =
                    mapOf(
                        "årsaker" to listOf(ÅrsakSettPåVent.REGISTRERING_AV_TILTAK.name),
                    )
                assertThat(metadata.tilJson()!!).isEqualTo(forventetMetadata)
            }
        }

        @Test
        fun `skal lagre kommentaren i metdata når man tar en behandling av vent`() {
            taAvVent()
            behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandling.id)
            with(behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).single()) {
                assertThat(this.utfall).isEqualTo(StegUtfall.TATT_AV_VENT)
                assertThat(metadata.tilJson()!!).isEmpty()
            }
        }
    }

    private fun sattPåVent() {
        behandlingshistorikkService.sattPåVent(
            behandling = behandling,
            kommentar = "en kommentar",
            årsaker = listOf(ÅrsakSettPåVent.REGISTRERING_AV_TILTAK),
        )
    }

    private fun taAvVent() {
        behandlingshistorikkService.taAvVent(
            behandling = behandling,
            kommentar = "tatt av vent",
        )
    }
}
