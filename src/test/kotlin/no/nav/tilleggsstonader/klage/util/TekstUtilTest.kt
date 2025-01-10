package no.nav.tilleggsstonader.klage.util

import no.nav.tilleggsstonader.klage.felles.util.TekstUtil.storForbokstav
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TekstUtilTest {

    @Test
    internal fun `stor forbokstav`() {
        assertThat("".storForbokstav()).isEqualTo("")
        assertThat("abc".storForbokstav()).isEqualTo("Abc")
        assertThat("ABC".storForbokstav()).isEqualTo("Abc")
        assertThat("ABC".storForbokstav()).isEqualTo("Abc")
    }
}
