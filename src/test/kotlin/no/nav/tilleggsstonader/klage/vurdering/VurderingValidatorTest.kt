package no.nav.tilleggsstonader.klage.vurdering

import no.nav.tilleggsstonader.klage.testutil.DomainUtil.vurderingDto
import no.nav.tilleggsstonader.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
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
                vurderingDto(
                    vedtak = Vedtak.OMGJØR_VEDTAK,
                    hjemler = null,
                    årsak = Årsak.FEIL_I_LOVANDVENDELSE,
                    begrunnelseOmgjøring = "begrunnelse",
                ),
            )
        }

        @Test
        internal fun `skal feile når årsak er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemler = null, årsak = null))
            }.hasMessage("Mangler årsak på omgjør vedtak")
        }

        @Test
        internal fun `skal feile når hjemmel ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurderingDto(
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                        årsak = Årsak.ANNET,
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                )
            }.hasMessage("Kan ikke lagre hjemmel på omgjør vedtak")
        }

        @Test
        internal fun `skal feile når begrunnelse for omgjøring er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, årsak = Årsak.ANNET, begrunnelseOmgjøring = null))
            }.hasMessage("Mangler begrunnelse for omgjøring på omgjør vedtak")
        }
    }

    @Nested
    inner class OpprettholdVedtak {
        @Test
        internal fun `skal validere når man har med hjemmel, men årsak er null`() {
            validerVurdering(vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN), årsak = null))
        }

        @Test
        internal fun `skal feile når hjemmel er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemler = null, årsak = null))
            }.hasMessage("Mangler hjemmel på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når årsak ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN), årsak = Årsak.ANNET),
                )
            }.hasMessage("Kan ikke lagre årsak på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når begrunnelse for omgjøring ikke er null`() {
            assertThatThrownBy {
                validerVurdering(
                    vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemler = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                )
            }.hasMessage("Kan ikke lagre begrunnelse på oppretthold vedtak")
        }
    }
}
