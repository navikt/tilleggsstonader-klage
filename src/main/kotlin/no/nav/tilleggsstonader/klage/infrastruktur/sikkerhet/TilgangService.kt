package no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet

import no.nav.tilleggsstonader.klage.behandling.BehandlingService
import no.nav.tilleggsstonader.klage.fagsak.FagsakService
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.felles.domain.AuditLogger
import no.nav.tilleggsstonader.klage.felles.domain.AuditLoggerEvent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlerRolle
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.CustomKeyValue
import no.nav.tilleggsstonader.klage.felles.domain.Sporingsdata
import no.nav.tilleggsstonader.klage.felles.dto.Tilgang
import no.nav.tilleggsstonader.klage.infrastruktur.config.FagsystemRolleConfig
import no.nav.tilleggsstonader.klage.infrastruktur.config.RolleConfig
import no.nav.tilleggsstonader.klage.infrastruktur.exception.ManglerTilgang
import no.nav.tilleggsstonader.klage.integrasjoner.TilleggsstonaderSakClient
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.spring.cache.getValue
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilgangService(
    private val tilleggsstonaderSakClient: TilleggsstonaderSakClient,
    private val rolleConfig: RolleConfig,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) {
    fun validerTilgangTilPersonMedRelasjonerForBehandling(
        behandlingId: BehandlingId,
        event: AuditLoggerEvent,
    ) {
        val personIdent =
            cacheManager.getValue("behandlingPersonIdent", behandlingId) {
                behandlingService.hentAktivIdent(behandlingId).first
            }

        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(
            Sporingsdata(
                event = event,
                personIdent = personIdent,
                tilgang = tilgang,
                custom1 = CustomKeyValue("behandling", behandlingId.toString()),
            ),
        )

        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til behandling=$behandlingId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    private fun harTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang =
        harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            tilleggsstonaderSakClient.sjekkTilgangTilPersonMedRelasjoner(personIdent)
        }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(
        cacheName: String,
        verdi: T,
        hentVerdi: () -> Tilgang,
    ): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler(true))) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerTilgangTilPersonMedRelasjonerForFagsak(
        fagsakId: UUID,
        event: AuditLoggerEvent,
    ) {
        val personIdent = hentFagsak(fagsakId).hentAktivIdent()

        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(
            Sporingsdata(
                event,
                personIdent,
                tilgang,
                custom1 = CustomKeyValue("fagsak", fagsakId.toString()),
            ),
        )
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=$fagsakId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    private fun hentFagsak(fagsakId: UUID): Fagsak =
        cacheManager.getValue("fagsak", fagsakId) {
            fagsakService.hentFagsak(fagsakId)
        }

    fun validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId: BehandlingId) {
        validerHarRolleForBehandling(behandlingId, BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerHarVeilederrolleTilStønadForBehandling(behandlingId: BehandlingId) {
        validerHarRolleForBehandling(behandlingId, BehandlerRolle.VEILEDER)
    }

    fun validerHarVeilederrolleTilStønadForFagsak(fagsakId: UUID) {
        harTilgangTilFagsakGittRolle(fagsakId, BehandlerRolle.VEILEDER)
    }

    private fun validerHarRolleForBehandling(
        behandlingId: BehandlingId,
        minumumRolle: BehandlerRolle,
    ) {
        if (!harTilgangTilBehandlingGittRolle(behandlingId, minumumRolle)) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang " +
                        "til å utføre denne operasjonen som krever minimumsrolle $minumumRolle",
                frontendFeilmelding = "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
            )
        }
    }

    fun harMinimumRolleTversFagsystem(minimumsrolle: BehandlerRolle): Boolean =
        harTilgangTilGittRolleForFagsystem(rolleConfig.ts, minimumsrolle)

    fun harTilgangTilBehandlingGittRolle(
        behandlingId: BehandlingId,
        minimumsrolle: BehandlerRolle,
    ): Boolean = harTilgangTilFagsakGittRolle(behandlingService.hentBehandling(behandlingId).fagsakId, minimumsrolle)

    fun harTilgangTilFagsakGittRolle(
        fagsakId: UUID,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val stønadstype = hentFagsak(fagsakId).fagsystem
        return harTilgangTilGittRolle(stønadstype, minimumsrolle)
    }

    private fun harTilgangTilGittRolle(
        fagsystem: Fagsystem,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val rolleForFagsystem =
            when (fagsystem) {
                Fagsystem.TILLEGGSSTONADER -> rolleConfig.ts
            }
        return harTilgangTilGittRolleForFagsystem(rolleForFagsystem, minimumsrolle)
    }

    private fun harTilgangTilGittRolleForFagsystem(
        fagsystemRolleConfig: FagsystemRolleConfig,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val rollerFraToken = SikkerhetContext.hentGrupperFraToken()
        val rollerForBruker =
            when {
                SikkerhetContext.hentSaksbehandler() == SikkerhetContext.SYSTEM_FORKORTELSE ->
                    listOf(
                        BehandlerRolle.SYSTEM,
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )

                rollerFraToken.contains(fagsystemRolleConfig.beslutter) ->
                    listOf(
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )

                rollerFraToken.contains(fagsystemRolleConfig.saksbehandler) ->
                    listOf(
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )

                rollerFraToken.contains(fagsystemRolleConfig.veileder) -> listOf(BehandlerRolle.VEILEDER)
                else -> listOf(BehandlerRolle.UKJENT)
            }

        return rollerForBruker.contains(minimumsrolle)
    }
}
