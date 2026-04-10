# Copilot Instructions – tilleggsstonader-klage

Backend-applikasjon for behandling av klager på vedtak relatert til tilleggsstønader (NAV). Kotlin/Spring Boot 4 med Spring Data JDBC og PostgreSQL.

## Bygg, test og lint

```bash
# Bygg
./gradlew clean build

# Kjør alle tester
./gradlew test

# Kjør én enkelt test
./gradlew test --tests "no.nav.tilleggsstonader.klage.behandling.StegServiceTest"

# Kjør én enkelt testmetode
./gradlew test --tests "no.nav.tilleggsstonader.klage.behandling.StegServiceTest.skalOppdatereSteg"

# Lint (ktlint via spotless)
./gradlew spotlessCheck

# Autofix lint
./gradlew spotlessApply

# Bygg uten lint
./gradlew build -PskipLint
```

Integrasjonstester bruker Testcontainers (PostgreSQL) og starter automatisk — Docker må kjøre.

## Arkitektur

### Steg-basert behandlingsflyt

En klagebehandling følger en sekvensiell pipeline definert i `StegType`:

```
OPPRETTET → FORMKRAV → VURDERING → BREV → OVERFØRING_TIL_KABAL → KABAL_VENTER_SVAR → BEHANDLING_FERDIGSTILT
```

Hvert steg har en tilhørende `BehandlingStatus`. `StegService` styrer overgangene og oppretter `Behandlingshistorikk`-innslag. Behandlingen er låst for redigering når status ikke er `OPPRETTET` eller `UTREDES`.

### Pakkestruktur

Koden er organisert etter domene under `no.nav.tilleggsstonader.klage`:

| Pakke | Ansvar |
|---|---|
| `behandling` | Kjernelogikk: opprettelse, stegflyt, ferdigstilling, henleggelse |
| `formkrav` | Validering av formelle krav (steg 1) |
| `vurdering` | Saksbehandlers vurdering med vedtak og hjemler (steg 2) |
| `brev` | Brevgenerering og signatur (steg 3) |
| `kabal` | Integrasjon mot Klageinstansen (Kabal) via REST og Kafka |
| `fagsak` | Fagsakdata fra upstream-systemer |
| `integrasjoner` | Klienter mot eksterne tjenester (PDL, Dokarkiv, etc.) |
| `infrastruktur` | Sikkerhet, database-konfig, repository-baser, exception handling |
| `felles` | Delte domeneklasser (`BehandlingId`, `Sporbar`, `BehandlerRolle`) |

### Kjerneentiteter

- **`Behandling`** — Hovedentitet med steg, status, resultat og påklaget vedtak
- **`Fagsak`/`FagsakDomain`** — Kobling til stønadstype og fagsystem
- **`Form`** — Formkravvurdering
- **`Vurdering`** — Saksbehandlers vurdering med vedtak (OMGJØR/OPPRETTHOLD) og hjemler
- **`Brev`** — Generert vedtaksbrev med avsnitt

## Konvensjoner

### Repository-mønster

Prosjektet bruker Spring Data JDBC (ikke JPA). Entiteter har forhåndssatte UUID-IDer, så `save()` er deaktivert — bruk eksplisitt `insert()` eller `update()`:

```kotlin
interface BehandlingRepository :
    RepositoryInterface<Behandling, BehandlingId>,
    InsertUpdateRepository<Behandling> {
    // ...
}

// Bruk:
repository.insert(behandling)   // Ny entitet
repository.update(behandling)   // Oppdater eksisterende
```

`RepositoryInterface` og `InsertUpdateRepository` i `infrastruktur.repository` er base-interfacene for alle repositories.

### Value classes for type-sikkerhet

Bruk `@JvmInline value class` for ID-typer i stedet for rå UUID:

```kotlin
@JvmInline
value class BehandlingId(val id: UUID) {
    companion object {
        fun random() = BehandlingId(UUID.randomUUID())
        fun fromString(id: String) = BehandlingId(UUID.fromString(id))
    }
}
```

### Asynkrone oppgaver (Tasks)

Bakgrunnsjobber bruker `familie-prosessering`-biblioteket:

```kotlin
@Service
@TaskStepBeskrivelse(
    taskStepType = MinTask.TYPE,
    beskrivelse = "Beskrivelse av oppgaven",
)
class MinTask : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        // ...
    }

    companion object {
        const val TYPE = "minTask"
    }
}
```

Payload er typisk en serialisert `BehandlingId`. Metadata legges i `TaskMetadata`-konstantene.

### Controller-mønster

Alle controllere bruker Azure AD-autentisering og tilgangskontroll:

```kotlin
@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
) {
    @GetMapping("{behandlingId}")
    fun hent(@PathVariable behandlingId: BehandlingId): BehandlingDto {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return behandlingService.hentBehandlingDto(behandlingId)
    }
}
```

### Auditing

Alle entiteter med `Sporbar` sporer hvem som opprettet/endret og når, via Spring Data JDBC auditing (`@EnableJdbcAuditing`).

### Database

- Flyway-migrasjoner i `src/main/resources/db/migration/`
- Navnekonvensjon: `V<nummer>__<snake_case_beskrivelse>.sql`
- Komplekse objekter lagres som JSON (PGobject) med custom converters i `DatabaseConfiguration`
- Placeholder `${ignoreIfProd}` brukes for å hoppe over dev-only SQL i produksjon

### Kabal-integrasjon

Klager sendes til Klageinstansen (Kabal) via REST (`KabalClient`). Resultater mottas via Kafka-topic `klage.behandling-events.v1` (`KabalKafkaListener`), som dispatcher events til `KabalBehandlingEventService`.

## Test

Integrasjonstester arver fra `IntegrationTest` som setter opp:
- Testcontainers PostgreSQL
- MockOAuth2Server for token-generering
- WireMock for HTTP-mocking av eksterne tjenester
- Automatisk database-reset mellom tester

Testdata opprettes med `DomainUtil`-factory-metoder og `TestoppsettService`:

```kotlin
class MinTest : IntegrationTest() {
    @Test
    fun test() {
        val behandling = testoppsettService.lagreBehandlingMedFagsak(DomainUtil.behandling())
        // ...
    }
}
```

### Spring-profiler

| Profil | Bruk |
|---|---|
| `local` | Lokal kjøring mot ekstern Postgres |
| `integrasjonstest` | Integrasjonstester |
| `mock-*` | Mocker for enkelttjenester (pdl, kabal, integrasjoner, etc.) |
| `dev` / `prod` | Miljøspesifikk konfigurasjon |
