package no.nav.tilleggsstonader.klage.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BrevsignaturServiceTest {
    val brevsignaturService = BrevsignaturService()

    @Test
    fun `skal anonymisere signatur hvis strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns Adressebeskyttelse.STRENGT_FORTROLIG

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysningerDto, Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.SIGNATUR_VIKAFOSSEN)
        assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.SIGNATUR_NAV_ANONYM_NAVN)
    }

    @Test
    internal fun `skal returnere saksehandlers navn og enhet hvis ikke strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signaturMedEnhet =
            testWithBrukerContext(preferredUsername = "Julenissen") {
                brevsignaturService.lagSignatur(personopplysningerDto, Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
            }

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.SIGNATUR_NAY)
        assertThat(signaturMedEnhet.navn).isEqualTo("Julenissen")
    }
}
