package no.nav.tilleggsstonader.klage.brev

import no.nav.tilleggsstonader.klage.brev.dto.SignaturDto
import no.nav.tilleggsstonader.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import org.springframework.stereotype.Service

@Service
class BrevsignaturService {

    fun lagSignatur(personopplysningerDto: PersonopplysningerDto): SignaturDto {
        val harStrengtFortroligAdresse: Boolean = personopplysningerDto.adressebeskyttelse?.erStrengtFortrolig() ?: false

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY)
        }
    }

    companion object {

        val NAV_ANONYM_NAVN = "NAV anonym"
        val ENHET_VIKAFOSSEN = "NAV Vikafossen"
        val ENHET_NAY = "NAV Arbeid og ytelser"
    }
}
