package no.nav.tilleggsstonader.klage.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext

class ByUserIdStrategy : Strategy {

    override fun getName(): String {
        return "byUserId"
    }

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        return map["user"]
            ?.split(',')
            ?.any { SikkerhetContext.hentSaksbehandler(strict = true) == it }
            ?: false
    }
}
