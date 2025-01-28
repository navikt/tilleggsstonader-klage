package no.nav.tilleggsstonader.klage.infrastruktur.config

import org.apache.commons.lang3.StringUtils

object PdlConfig {
    const val PATH_GRAPHQL = "graphql"

    val søkerQuery = graphqlQuery("/pdl/søker.graphql")

    val bolkNavnQuery = graphqlQuery("/pdl/navn_bolk.graphql")

    val hentIdentQuery = graphqlQuery("/pdl/hent_ident.graphql")

    private fun graphqlQuery(path: String) =
        PdlConfig::class.java
            .getResource(path)!!
            .readText()
            .graphqlCompatible()

    private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
}
