package no.nav.tilleggsstonader.klage.infrastruktur.exception

open class PdlRequestException(
    melding: String? = null,
) : Exception(melding)

class PdlNotFoundException : PdlRequestException()
