package no.nav.tilleggsstonader.klage.behandlingshistorikk.domain

import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val steg: StegType,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime? = LocalDateTime.now(),
)
