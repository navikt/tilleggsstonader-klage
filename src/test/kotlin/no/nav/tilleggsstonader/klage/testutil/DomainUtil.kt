package no.nav.tilleggsstonader.klage.testutil

import no.nav.tilleggsstonader.klage.behandling.domain.Behandling
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtak
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakstype
import no.nav.tilleggsstonader.klage.behandling.domain.StegType
import no.nav.tilleggsstonader.klage.behandling.dto.PåklagetVedtakDto
import no.nav.tilleggsstonader.klage.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.klage.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.klage.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import no.nav.tilleggsstonader.klage.felles.domain.Sporbar
import no.nav.tilleggsstonader.klage.felles.domain.SporbarUtils
import no.nav.tilleggsstonader.klage.felles.util.tilFagsystem
import no.nav.tilleggsstonader.klage.formkrav.domain.Form
import no.nav.tilleggsstonader.klage.formkrav.domain.FormVilkår
import no.nav.tilleggsstonader.klage.formkrav.domain.FormkravFristUnntak
import no.nav.tilleggsstonader.klage.infrastruktur.config.DatabaseConfiguration
import no.nav.tilleggsstonader.klage.kabal.domain.KlageinstansResultat
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemler
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemmel
import no.nav.tilleggsstonader.klage.vurdering.domain.Vedtak
import no.nav.tilleggsstonader.klage.vurdering.domain.Vurdering
import no.nav.tilleggsstonader.klage.vurdering.dto.VurderingDto
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.RelevantDato
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingEventType
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingResultat
import no.nav.tilleggsstonader.kontrakter.klage.BehandlingStatus
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.HenlagtÅrsak
import no.nav.tilleggsstonader.kontrakter.klage.KlageinstansUtfall
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import no.nav.tilleggsstonader.kontrakter.klage.Årsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

object DomainUtil {
    fun fagsakDomain(
        id: UUID = UUID.randomUUID(),
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
        personId: UUID = UUID.randomUUID(),
        fagsystem: Fagsystem = Fagsystem.TILLEGGSSTONADER,
        eksternId: String = Random.nextInt().toString(),
    ): FagsakDomain =
        FagsakDomain(
            id = id,
            fagsakPersonId = personId,
            stønadstype = stønadstype,
            eksternId = eksternId,
            fagsystem = fagsystem,
        )

    fun FagsakDomain.tilFagsak(personIdent: String = "11223344551") = this.tilFagsakMedPerson(setOf(PersonIdent(ident = personIdent)))

    fun behandling(
        fagsak: Fagsak = fagsak(),
        id: BehandlingId = BehandlingId.random(),
        eksternBehandlingId: UUID = UUID.randomUUID(),
        påklagetVedtak: PåklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.IKKE_VALGT, null),
        klageMottatt: LocalDate = LocalDate.now(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        steg: StegType = StegType.FORMKRAV,
        behandlendeEnhet: String = "4462",
        resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
        vedtakDato: LocalDateTime? = null,
        henlagtÅrsak: HenlagtÅrsak? = null,
        henlagtBegrunnelse: String? = null,
        sporbar: Sporbar = Sporbar(),
        fagsystemRevurdering: FagsystemRevurdering? = null,
    ): Behandling =
        Behandling(
            id = id,
            eksternBehandlingId = eksternBehandlingId,
            fagsakId = fagsak.id,
            påklagetVedtak = påklagetVedtak,
            klageMottatt = klageMottatt,
            status = status,
            steg = steg,
            behandlendeEnhet = behandlendeEnhet,
            resultat = resultat,
            henlagtÅrsak = henlagtÅrsak,
            henlagtBegrunnelse = henlagtBegrunnelse,
            vedtakDato = vedtakDato,
            sporbar = sporbar,
            fagsystemRevurdering = fagsystemRevurdering,
        )

    fun vurdering(
        behandlingId: BehandlingId,
        vedtak: Vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        hjemler: List<Hjemmel>? = listOf(Hjemmel.FS_TILL_ST_10_TILSYN),
        årsak: Årsak? = null,
        begrunnelseOmgjøring: String? = null,
        interntNotat: String? = null,
    ) = Vurdering(
        behandlingId = behandlingId,
        vedtak = vedtak,
        hjemler = hjemler?.let { Hjemler(hjemler) },
        innstillingKlageinstans = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        årsak = årsak,
        begrunnelseOmgjøring = begrunnelseOmgjøring,
        interntNotat = interntNotat,
    )

    fun vurderingDto(
        behandlingId: BehandlingId = BehandlingId.random(),
        vedtak: Vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        årsak: Årsak? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) null else Årsak.FEIL_I_LOVANDVENDELSE,
        begrunnelseOmgjøring: String? = null,
        hjemler: List<Hjemmel>? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) listOf(Hjemmel.FS_TILL_ST_10_TILSYN) else null,
        innstillingKlageinstans: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        interntNotat: String? = null,
    ) = VurderingDto(
        behandlingId = behandlingId,
        vedtak = vedtak,
        årsak = årsak,
        begrunnelseOmgjøring = begrunnelseOmgjøring,
        hjemler = hjemler,
        innstillingKlageinstans = innstillingKlageinstans,
        interntNotat = interntNotat,
    )

    fun oppfyltForm(behandlingId: BehandlingId) =
        Form(
            behandlingId = behandlingId,
            klagePart = FormVilkår.OPPFYLT,
            klagefristOverholdt = FormVilkår.OPPFYLT,
            klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_SATT,
            klageKonkret = FormVilkår.OPPFYLT,
            klageSignert = FormVilkår.OPPFYLT,
        )

    val defaultIdenter = setOf(PersonIdent("01010199999"))

    fun fagsak(
        identer: Set<PersonIdent> = defaultIdenter,
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
        id: UUID = UUID.randomUUID(),
        sporbar: Sporbar = Sporbar(),
        fagsakPersonId: UUID = UUID.randomUUID(),
    ): Fagsak = fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), sporbar)

    fun fagsak(
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
        id: UUID = UUID.randomUUID(),
        person: FagsakPerson,
        sporbar: Sporbar = Sporbar(),
    ): Fagsak =
        Fagsak(
            id = id,
            fagsakPersonId = person.id,
            personIdenter = person.identer,
            stønadstype = stønadstype,
            sporbar = sporbar,
            eksternId = "1",
            fagsystem = stønadstype.tilFagsystem(),
        )

    fun klageresultat(
        eventId: UUID = UUID.randomUUID(),
        type: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        utfall: KlageinstansUtfall = KlageinstansUtfall.MEDHOLD,
        mottattEllerAvsluttetTidspunkt: LocalDateTime = SporbarUtils.now(),
        kildereferanse: UUID = UUID.randomUUID(),
        journalpostReferanser: List<String> = listOf("1", "2"),
        behandlingId: BehandlingId = BehandlingId.random(),
    ): KlageinstansResultat =
        KlageinstansResultat(
            eventId = eventId,
            type = type,
            utfall = utfall,
            mottattEllerAvsluttetTidspunkt = mottattEllerAvsluttetTidspunkt,
            kildereferanse = kildereferanse,
            journalpostReferanser = DatabaseConfiguration.StringListWrapper(verdier = journalpostReferanser),
            behandlingId = behandlingId,
        )

    fun journalpost(
        dokumenter: List<DokumentInfo> = emptyList(),
        relevanteDatoer: List<RelevantDato> = emptyList(),
    ) = Journalpost(
        journalpostId = UUID.randomUUID().toString(),
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = "ENF",
        behandlingstema = null,
        tittel = "Tut og kjør",
        sak = null,
        bruker = null,
        avsenderMottaker = null,
        journalforendeEnhet = null,
        kanal = null,
        dokumenter = dokumenter,
        relevanteDatoer = relevanteDatoer,
        eksternReferanseId = null,
    )

    fun journalpostDokument(
        status: Dokumentstatus = Dokumentstatus.FERDIGSTILT,
        dokumentvarianter: List<Dokumentvariant>? =
            listOf(
                Dokumentvariant(
                    Dokumentvariantformat.ARKIV,
                    saksbehandlerHarTilgang = true,
                ),
            ),
    ) = DokumentInfo(
        dokumentInfoId = UUID.randomUUID().toString(),
        tittel = "Tittel",
        brevkode = null,
        dokumentstatus = status,
        dokumentvarianter = dokumentvarianter,
        logiskeVedlegg = listOf(),
    )

    fun påklagetVedtakDetaljer(
        eksternFagsystemBehandlingId: String = "123",
        vedtakstidspunkt: LocalDateTime = LocalDate.of(2022, 3, 1).atTime(8, 0),
    ) = PåklagetVedtakDetaljer(
        fagsystemType = FagsystemType.ORDNIÆR,
        eksternFagsystemBehandlingId = eksternFagsystemBehandlingId,
        behandlingstype = "type",
        resultat = "resultat",
        vedtakstidspunkt = vedtakstidspunkt,
        regelverk = Regelverk.NASJONAL,
    )

    fun påklagetVedtakDto(): PåklagetVedtakDto =
        PåklagetVedtakDto(eksternFagsystemBehandlingId = null, påklagetVedtakstype = PåklagetVedtakstype.UTEN_VEDTAK)

    fun personopplysningerDto(
        personIdent: String = "123",
        adressebeskyttelse: Adressebeskyttelse? = null,
    ) = PersonopplysningerDto(
        personIdent = personIdent,
        navn = "navn",
        adressebeskyttelse = adressebeskyttelse,
        folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
        dødsdato = null,
        egenAnsatt = false,
        vergemål = emptyList(),
        harFullmektig = true,
    )

    fun fagsystemVedtak(
        eksternBehandlingId: String,
        behandlingstype: String = "type",
        resultat: String = "resultat",
        vedtakstidspunkt: LocalDateTime = LocalDate.of(2022, 3, 1).atTime(8, 0),
    ) = FagsystemVedtak(
        fagsystemType = FagsystemType.ORDNIÆR,
        eksternBehandlingId = eksternBehandlingId,
        behandlingstype = behandlingstype,
        resultat = resultat,
        vedtakstidspunkt = vedtakstidspunkt,
        regelverk = Regelverk.NASJONAL,
    )
}
