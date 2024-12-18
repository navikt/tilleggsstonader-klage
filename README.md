# tilleggsstonader-klage
App for behandling av klager relatert til tilleggsstønader

## Bygging lokalt
Appen kjører på JRE 21. Bygging gjøres ved å kjøre `mvn clean install`.

### Autentisering lokalt
Dersom man vil gjøre autentiserte kall mot andre tjenester, eller vil kjøre applikasjonen sammen med frontend, må man sette opp følgende miljø-variabler:

#### Client id & client secret
secret kan hentes fra cluster med
```
kubectl -n tilleggsstonader get secret azuread-tilleggsstonader-klage-lokal -o json | jq '.data | map_values(@base64d)'
```

* `AZURE_APP_CLIENT_ID` (fra secret)
* `AZURE_APP_CLIENT_SECRET` (fra secret)
* Scope for den aktuelle tjenesten (`FAMILIE_INTEGRASJONER_SCOPE`, `FAMILIE_OPPDRAG_SCOPE`)

Variablene legges inn under ApplicationLocal -> Edit Configurations -> Environment Variables.

### Kjøring med in-memory-database
For å kjøre opp appen lokalt, kan en kjøre `ApplicationLocal`.

Appen starter da opp med en in memory-database og er da tilgjengelig under `localhost:8093`.
Databasen kan aksesseres på `localhost:8093/h2-console`. Log på jdbc url `jdbc:h2:mem:testdb` med bruker `sa` og blankt passord.

### Kjøring med postgres-database
For å kjøre opp appen lokalt med en postgres-database, kan en kjøre `KlageAppLocalPostgres`.
App'en vil starte opp en container med siste versjon av postgres.

For å kjøre opp postgres containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volumen `docker-compose down -v`

### Kjøring med brev
Vanlgvis kjøres appen opp med mockede versjoner av `familie-brev` og `familie-dokument`. 
For å kjøre opp med lokale versjoner av disse appene kan en kommentere ut `mock-brev` og `mock-dokument` i `KlageAppLocalPostgres`.
Deretter kan en kjøre opp appen `familie-brev` i brancen `klage-config` og appen `familie-dokument` i brancen `mocket-auth-server`.
I `familie-dokument` må en kjøre `DevLauncherMedMockServer`. 

### Dummy-svar fra kabal ved lokal kjøring
Lokalt finnes ingen kafka, så for å kunne generere et svar fra kabal kan man bruke endepunktene i `TestHendelseController`. 
* `POST mot http://localhost:8090/api/test/kabal/<behandling_id>/dummy`, med curl eller i postman (uten Authorization-header for å unngå at man prøver å sende inn det som saksbehandler)

## Produksjonssetting
Applikasjonen vil deployes til produksjon ved ny commit på master-branchen. Det er dermed tilstrekkelig å merge PR for å trigge produksjonsbygget.

## Roller
Testbrukeren som opprettes i IDA må ha minst en av følgende roller:
- 0000-GA-Tilleggsstonader-Beslutter
- 0000-GA-Tilleggsstonader-Saksbehandler

## Testdata
- Registering av arbeidssøker - https://arbeidssokerregistrering.dev.nav.no/

### Koble til database i preprod:
[Oppskrift på databasetilkobling](https://github.com/navikt/tilleggsstonader/blob/main/doc/dev/database.md)

For å koble til dev-db kan du kjøre kommandoene:
1. `gcloud auth login`
2. `gcp-db tilleggsstonader-dev-371d tilleggsstonader-klage`
3. Url: `jdbc:postgresql://localhost:5432/tilleggsstonader-klage`
4. Brukernavn: `fornavn.etternavn@nav.no` som brukernavn
5. Passord: Lim inn det som ligger i clipboard fra steg 2
6. Har du ikke lagt til deg selv som database-bruker må du gjør [dette først](https://doc.nais.io/persistence/postgres/)

## Ny fagsystemløsning
#### I klage
1. Må legge inn fagsystemet i inbound og outbound i `dev.yaml` og `prod.yaml`
2. Legge inn eventuelle roller som kreves for å kunne saksbehandle gitt stønad i `TilgangService.harTilgangTilGittRolleForFagsystem`
3. Se over felter som sendes til DVH
4. Se over brevtekster
5. Se over enhet i brevsignatur 

### Familie-brev
1. Må eventuellt oppdatere enums i familie-brev (som genererer en pdf-blankett av klagebehandlingen)

#### I fagsystemet
1. Fagsystemet må implementere et endepunkt for å hente ut alle vedtak
   1. `fagsystemUrl/api/ekstern/vedtak` som returnerer `Ressurs<List<FagsystemVedtak>>`
      - Ta stilling til om endepunkten burde returnere vedtak fra tilbakekreving
3. For å opprette en klage må fagsystemet kalle på `api/ekstern/behandling/opprett` med `OpprettKlagebehandlingRequest`. 
4. For å hente ut alle klager må fagsystemet kalle på `api/ekstern/behandling/{fagsystem}?eksternFagsakIder={eksternFagsakId}`

# Henvendelser


Spørsmål knyttet til koden eller prosjektet kan rettes til:

* Viktor Grøndalen Solberg, `viktor.grondalen.solberg@nav.no`
* Eirik Årseth `eirik.arseth@nav.no`

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po_aap_tilleggsstønader
