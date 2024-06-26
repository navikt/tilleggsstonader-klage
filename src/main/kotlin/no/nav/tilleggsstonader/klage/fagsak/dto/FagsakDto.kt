package no.nav.tilleggsstonader.klage.fagsak.dto

import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import java.util.UUID

data class FagsakDto(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdent: String,
    val stønadstype: Stønadstype,
    val eksternId: String,
    val fagsystem: Fagsystem,
)

fun Fagsak.tilDto(): FagsakDto =
    FagsakDto(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personIdent = this.hentAktivIdent(),
        stønadstype = stønadstype,
        eksternId = this.eksternId,
        fagsystem = this.fagsystem,
    )
