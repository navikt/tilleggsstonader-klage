package no.nav.tilleggsstonader.klage.testutil

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.infrastruktur.featuretoggle.FeatureToggleService

fun mockFeatureToggleService(enabled: Boolean = true): FeatureToggleService {
    val mockk = mockk<FeatureToggleService>()
    every { mockk.isEnabled(any()) } returns enabled
    return mockk
}
