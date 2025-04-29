package no.nav.tilleggsstonader.klage.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstonaderSakClient
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.IkkeOpprettet
import no.nav.tilleggsstonader.kontrakter.klage.IkkeOpprettetÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KanOppretteRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.OpprettRevurderingResponse
import no.nav.tilleggsstonader.kontrakter.klage.Opprettet
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

@Configuration
@Profile("mock-tilleggsstonader-sak")
class TilleggsstonaderSakClientMock {
    @Bean
    @Primary
    fun hentVedtak(): TilleggsstonaderSakClient = resetMock(mockk())

    companion object {
        fun resetMock(mock: TilleggsstonaderSakClient): TilleggsstonaderSakClient {
            clearMocks(mock)

            every { mock.erEgenAnsatt(any()) } returns false

            every { mock.sjekkTilgangTilPerson(any<String>()) } returns
                Tilgang(
                    harTilgang = true,
                    begrunnelse = null,
                )

            every { mock.sjekkTilgangTilPerson(any<IdentStønadstype>()) } returns
                Tilgang(
                    harTilgang = true,
                    begrunnelse = null,
                )

            every { mock.hentVedtak(any()) } returns
                listOf(
                    FagsystemVedtak(
                        "123",
                        "Førstegangsbehandling",
                        "Innvilget",
                        vedtakstidspunkt = LocalDateTime.of(2022, Month.AUGUST, 1, 8, 0),
                        fagsystemType = FagsystemType.ORDNIÆR,
                        regelverk = Regelverk.NASJONAL,
                    ),
                    FagsystemVedtak(
                        "124",
                        "Revurdering",
                        "Opphørt",
                        vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 0),
                        fagsystemType = FagsystemType.ORDNIÆR,
                        regelverk = Regelverk.NASJONAL,
                    ),
                    FagsystemVedtak(
                        "tilbake-123",
                        "Tilbakekreving",
                        "Full tilbakekreving",
                        vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 10, 2),
                        fagsystemType = FagsystemType.ORDNIÆR,
                        regelverk = Regelverk.NASJONAL,
                    ),
                    FagsystemVedtak(
                        "sanksjon-123",
                        "Revurdering",
                        "Sanksjon 1 måned",
                        vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 15, 2),
                        fagsystemType = FagsystemType.ORDNIÆR,
                        regelverk = Regelverk.NASJONAL,
                    ),
                )
            // mocker annen hver
            var opprettet = true
            every { mock.opprettRevurdering(any()) } answers {
                opprettet = !opprettet
                if (opprettet) {
                    OpprettRevurderingResponse(Opprettet(eksternBehandlingId = UUID.randomUUID().toString()))
                } else {
                    OpprettRevurderingResponse(IkkeOpprettet(årsak = IkkeOpprettetÅrsak.ÅPEN_BEHANDLING))
                }
            }

            var kanOpprette = true
            every { mock.kanOppretteRevurdering(any()) } answers {
                kanOpprette = !kanOpprette
                if (kanOpprette) {
                    KanOppretteRevurderingResponse(true, null)
                } else {
                    KanOppretteRevurderingResponse(false, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
                }
            }

            return mock
        }
    }
}
