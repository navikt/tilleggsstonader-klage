# tilleggsstonader-klage

App for behandling av klager relatert til tilleggsstønader

## Kjøre opp appen lokalt

Appen kjører på JRE 21. Bygging gjøres ved å kjøre `./gradlew clean install`

### Autentisering lokalt

Dersom man vil gjøre autentiserte kall mot andre tjenester, eller vil kjøre applikasjonen sammen med frontend, må man
sette opp følgende miljø-variabler:

* `AZURE_APP_CLIENT_ID` (fra secret)
* `AZURE_APP_CLIENT_SECRET` (fra secret)

Secret kan hentes fra cluster med

```
kubectl -n tilleggsstonader get secret azuread-tilleggsstonader-klage-lokal -o json | jq '.data | map_values(@base64d)'
```

Variablene legges inn under ApplicationLocal -> Edit Configurations -> Environment Variables.

### Kjøring med in-memory-database

For å kjøre opp appen lokalt, kan en kjøre `KlageAppLocal`.

Appen starter da opp med en in memory-database og er da tilgjengelig under `localhost:8093`.
Databasen kan aksesseres på `localhost:8093/h2-console`. Log på jdbc url `jdbc:h2:mem:testdb` med bruker `sa` og blankt
passord.

* Hvis man ønsker å bruke samme oppgaver som brukes i tilleggsstonader-sak så kan man kalle på sak for å hente/opprette oppgaver 
  * Kommenter ut `mock-oppgave`
  * Kommenter inn `bruk-sak-oppgave`

### Kjøring med postgres-database

For å kjøre opp appen lokalt med en postgres-database, kan en kjøre `KlageAppLocalPostgres`.
App'en vil starte opp en container med siste versjon av postgres.

For å kjøre opp postgres containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volumen `docker-compose down -v`

### Dummy-svar fra kabal ved lokal kjøring

Lokalt finnes ingen kafka, så for å kunne generere et svar fra kabal kan man bruke endepunktene i
`TestHendelseController`.

* `POST mot http://localhost:8090/api/test/kabal/<behandling_id>/dummy`, med curl eller i postman (uten
  Authorization-header for å unngå at man prøver å sende inn det som saksbehandler)

## Produksjonssetting

Applikasjonen vil deployes til produksjon ved ny commit på master-branchen. Det er dermed tilstrekkelig å merge PR for å
trigge produksjonsbygget.

## Roller

Testbrukeren som opprettes i IDA må ha minst en av følgende roller:

- 0000-GA-Tilleggsstonader-Beslutter
- 0000-GA-Tilleggsstonader-Saksbehandler

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan opprettes under [Issues](https://github.com/navikt/tilleggsstonader-klage/issues) her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po_aap_tilleggsstønader

## Kode generert av GitHub Copilot
Dette repoet bruker GitHub Copilot til å generere kode.
