package no.nav.tilleggsstonader.klage.kabal

import no.nav.tilleggsstonader.klage.felles.domain.SporbarUtils
import no.nav.tilleggsstonader.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.tilleggsstonader.klage.infrastruktur.config.IntegrationTest
import no.nav.tilleggsstonader.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsak
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class KlageinstansResultatRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    @Test
    internal fun `skal kunne lagre og hente klageresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
        val klageinstansResultat =
            KlageinstansResultat(
                eventId = UUID.randomUUID(),
                type = BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                utfall = KlageinstansUtfall.OPPHEVET,
                mottattEllerAvsluttetTidspunkt = SporbarUtils.now(),
                kildereferanse = UUID.randomUUID(),
                journalpostReferanser = StringListWrapper(listOf("ref1", "ref2")),
                behandlingId = behandling.id,
            )
        klageresultatRepository.insert(klageinstansResultat)

        val hentetKlageresultat = klageresultatRepository.findByIdOrThrow(klageinstansResultat.eventId)
        assertThat(hentetKlageresultat.eventId).isEqualTo(klageinstansResultat.eventId)
        assertThat(hentetKlageresultat.type).isEqualTo(klageinstansResultat.type)
        assertThat(hentetKlageresultat.utfall).isEqualTo(klageinstansResultat.utfall)
        assertThat(hentetKlageresultat.mottattEllerAvsluttetTidspunkt).isEqualTo(klageinstansResultat.mottattEllerAvsluttetTidspunkt)
        assertThat(hentetKlageresultat.kildereferanse).isEqualTo(klageinstansResultat.kildereferanse)
        assertThat(hentetKlageresultat.journalpostReferanser).isEqualTo(klageinstansResultat.journalpostReferanser)
        assertThat(hentetKlageresultat.journalpostReferanser.verdier).hasSize(2)
        assertThat(hentetKlageresultat.behandlingId).isEqualTo(klageinstansResultat.behandlingId)
    }
}
