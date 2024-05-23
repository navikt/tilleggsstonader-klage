package no.nav.tilleggsstonader.klage.infrastruktur.featuretoggle

import no.nav.tilleggsstonader.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(val unleashService: UnleashService) {

    fun isEnabled(toggle: Toggle): Boolean {
        return unleashService.isEnabled(toggle.toggleId, false)
    }

    fun isEnabled(toggle: Toggle, defaultVerdi: Boolean): Boolean {
        return unleashService.isEnabled(toggle.toggleId, defaultVerdi)
    }
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    PLACEHOLDER("ktlint-liker-ikke-tomme-enums"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
