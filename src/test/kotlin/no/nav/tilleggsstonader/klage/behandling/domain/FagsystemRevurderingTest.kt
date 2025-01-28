package no.nav.tilleggsstonader.klage.behandling.domain

import org.junit.jupiter.api.Test

internal class FagsystemRevurderingTest {
    @Test
    internal fun `skal kunne mappe enums`() {
        no.nav.tilleggsstonader.kontrakter.klage.IkkeOpprettetÅrsak.entries.forEach {
            IkkeOpprettetÅrsak.valueOf(it.name)
        }
    }
}
