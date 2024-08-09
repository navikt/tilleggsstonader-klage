package no.nav.tilleggsstonader.klage.kabal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.infrastruktur.config.LenkeConfig
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstønaderIntegrasjonerClient
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.behandling
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.fagsakDomain
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class KabalServiceTest {

    val kabalClient = mockk<KabalClient>()
    val integrasjonerClient = mockk<TilleggsstønaderIntegrasjonerClient>()
    val lenkeConfig = LenkeConfig(tilleggsstonaderSakLenke = "SAK_FRONTEND_URL")
    val kabalService = KabalService(kabalClient, integrasjonerClient, lenkeConfig)
    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

    val hjemmel = Hjemmel.FS_TILL_ST_10_TILSYN

    val oversendelseSlot = slot<OversendtKlageAnkeV3>()
    val saksbehandlerA = Saksbehandler(UUID.randomUUID(), "A123456", "Alfa", "Surname", "4462")
    val saksbehandlerB = Saksbehandler(UUID.randomUUID(), "B987654", "Beta", "Etternavn", "4462")

    @BeforeEach
    internal fun setUp() {
        every { kabalClient.sendTilKabal(capture(oversendelseSlot)) } just Runs
        every { integrasjonerClient.hentSaksbehandlerInfo(any()) } answers {
            when (firstArg<String>()) {
                saksbehandlerA.navIdent -> saksbehandlerA
                saksbehandlerB.navIdent -> saksbehandlerB
                else -> error("Fant ikke info om saksbehanlder ${firstArg<String>()}")
            }
        }
    }

    @Test
    fun sendTilKabal() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerA.navIdent)

        val oversendelse = oversendelseSlot.captured
        assertThat(oversendelse.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(oversendelse.fagsak?.fagsystem).isEqualTo(Fagsystem.TILLEGGSSTONADER)
        assertThat(oversendelse.hjemler).containsAll(listOf(hjemmel.kabalHjemmel))
        assertThat(oversendelse.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
        assertThat(oversendelse.innsynUrl)
            .isEqualTo("${lenkeConfig.tilleggsstonaderSakLenke}/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}")
        assertThat(oversendelse.tilknyttedeJournalposter).isEmpty()
        assertThat(oversendelse.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.innsendtTilNav).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
        assertThat(oversendelse.sakenGjelder).isNull()
        assertThat(oversendelse.kilde).isEqualTo(Fagsystem.TILLEGGSSTONADER)
        assertThat(oversendelse.ytelse).isEqualTo(Ytelse.TSO_TSO)
        assertThat(oversendelse.kommentar).isNull()
        assertThat(oversendelse.dvhReferanse).isNull()
        assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerA.enhet)
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtak ikke er satt`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent)

        assertThat(oversendelseSlot.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.tilleggsstonaderSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
        assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerB.enhet)
    }

    @Disabled // tar ut test til azure graph-feil er løst
    @Test
    internal fun `skal feile hvis saksbehandlerinfo ikke finnes`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        assertThrows<IllegalStateException> {
            kabalService.sendTilKabal(fagsak, behandling, vurdering, "UKJENT1234")
        }
    }
}
