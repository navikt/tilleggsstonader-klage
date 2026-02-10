package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurderingDto
import no.nav.tilleggsstonader.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class VurderingValidatorTest {
    @Nested
    inner class OmgjørVedtak {
        @Test
        internal fun `skal validere når man har med årsak, men hjemmel er null`() {
            validerVurdering(
                vurdering =
                    vurderingDto(
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        hjemler = null,
                        årsak = Årsak.FEIL_I_LOVANDVENDELSE,
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                stønadstype = Stønadstype.BARNETILSYN,
            )
        }

        @Test
        internal fun `skal feile når årsak er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemler = null, årsak = null),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Mangler årsak på omgjør vedtak")
        }

        @Test
        internal fun `skal feile når hjemmel ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OMGJØR_VEDTAK,
                            hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                            årsak = Årsak.ANNET,
                            begrunnelseOmgjøring = "begrunnelse",
                        ),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Kan ikke lagre hjemmel på omgjør vedtak")
        }

        @Test
        internal fun `skal feile når begrunnelse for omgjøring er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OMGJØR_VEDTAK,
                            årsak = Årsak.ANNET,
                            begrunnelseOmgjøring = null,
                        ),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Mangler begrunnelse for omgjøring på omgjør vedtak")
        }
    }

    @Nested
    inner class OpprettholdVedtak {
        @Test
        internal fun `skal validere når man har med hjemmel, men årsak er null`() {
            validerVurdering(
                vurdering =
                    vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                        årsak = null,
                    ),
                stønadstype = Stønadstype.BARNETILSYN,
            )
        }

        @Test
        internal fun `skal feile når hjemmel er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemler = null, årsak = null),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Mangler hjemmel på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når årsak ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                            hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                            årsak = Årsak.ANNET,
                        ),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Kan ikke lagre årsak på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når begrunnelse for omgjøring ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                            hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                            begrunnelseOmgjøring = "begrunnelse",
                        ),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage("Kan ikke lagre begrunnelse på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile hvis nay bruker hjemmel som kun er godtkjent for tiltaksenheten`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                            hjemler = listOf(Hjemmel.FS_TILL_ST_5),
                            begrunnelseOmgjøring = null,
                        ),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }.hasMessage(
                "En eller flere hjemler kan ikke brukes når behandlende enhet er NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD: FS_TILL_ST_5",
            )
        }

        @Test
        internal fun `skal feile hvis tiltaksenheten bruker hjemmel som kun er godtkjent for nay`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering =
                        vurderingDto(
                            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                            hjemler = listOf(Hjemmel.FTRL_21_12),
                            begrunnelseOmgjøring = null,
                        ),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                )
            }.hasMessage("En eller flere hjemler kan ikke brukes når behandlende enhet er NAV_TILTAK_OSLO: FTRL_21_12")
        }
    }
}
