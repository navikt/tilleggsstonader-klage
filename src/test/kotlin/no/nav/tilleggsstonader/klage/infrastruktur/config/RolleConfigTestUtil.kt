package no.nav.tilleggsstonader.klage.infrastruktur.config

object RolleConfigTestUtil {

    val rolleConfig = RolleConfig(
        ba = FagsystemRolleConfig("baSaksbehandler", "baBeslutter", "baVeileder"),
        ef = FagsystemRolleConfig("efSaksbehandler", "efBeslutter", "efVeileder"),
        ks = FagsystemRolleConfig("ksSaksbehandler", "ksBeslutter", "ksVeileder"),
        ts = FagsystemRolleConfig("tsSaksbehandler", "tsBeslutter", "tsVeileder"),
    )
}
