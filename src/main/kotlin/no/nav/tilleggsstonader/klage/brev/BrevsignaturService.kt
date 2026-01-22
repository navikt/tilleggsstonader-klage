package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.dto.SignaturDto
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import org.springframework.stereotype.Service

@Service
class BrevsignaturService {
    fun lagSignatur(
        personopplysningerDto: PersonopplysningerDto,
        enhet: Enhet,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean = personopplysningerDto.adressebeskyttelse?.erStrengtFortrolig() == true

        return if (harStrengtFortroligAdresse) {
            SignaturDto(SIGNATUR_NAV_ANONYM_NAVN, SIGNATUR_VIKAFOSSEN)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), enhet.tilSignatur())
        }
    }

    fun Enhet.tilSignatur() =
        when (this) {
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD -> SIGNATUR_NAY
            Enhet.NAV_TILTAK_OSLO -> SIGNATUR_TILTAKSENHETEN
            Enhet.VIKAFOSSEN -> SIGNATUR_VIKAFOSSEN
            Enhet.NAV_ARBEID_OG_YTELSER_EGNE_ANSATTE -> error("Har ikke signatur for $this")
        }

    companion object {
        const val SIGNATUR_NAV_ANONYM_NAVN = "Nav anonym"
        const val SIGNATUR_VIKAFOSSEN = "Nav Vikafossen"
        const val SIGNATUR_NAY = "Nav Arbeid og ytelser"
        const val SIGNATUR_TILTAKSENHETEN = "Nav Tiltak Oslo"
    }
}
