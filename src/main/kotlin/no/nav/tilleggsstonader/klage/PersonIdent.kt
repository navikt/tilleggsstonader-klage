package no.nav.tilleggsstonader.klage

import jakarta.validation.constraints.Pattern

data class PersonIdent(
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig") val ident: String,
)
