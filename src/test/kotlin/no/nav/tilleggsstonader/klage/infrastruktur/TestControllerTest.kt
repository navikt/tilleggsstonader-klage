package no.nav.tilleggsstonader.klage.infrastruktur

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.klage.IntegrationTest
import no.nav.tilleggsstonader.libs.test.httpclient.ProblemDetailUtil.catchProblemDetailException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpServerErrorException.InternalServerError
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *
 */
class TestControllerTest : IntegrationTest() {
    val json = """{"tekst":"abc","dato":"2023-01-01","tidspunkt":"2023-01-01T12:00:03"}"""
    val feilJson =
        """{"type":"about:blank","title":"Internal Server Error","status":500,"detail":"Ukjent feil","instance":"/api/test/error"}"""

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    fun `skal kunne hente json fra endepunkt`() {
        val response =
            restTemplate.exchange<String>(localhost("/api/test"), HttpMethod.GET, HttpEntity(null, headers))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!).isEqualTo(json)
    }

    @Test
    fun `skal kunne sende inn object`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )

        val request = HttpEntity(json, headers)
        val response = restTemplate.postForEntity<TestObject>(localhost("/api/test"), request)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!).isEqualTo(json)
    }

    @Test
    fun `skal kunne sende inn object med json header`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )
        val jsonHeaders =
            HttpHeaders().apply {
                contentType = APPLICATION_JSON
                accept = listOf(APPLICATION_JSON)
                setBearerAuth(lokalTestToken)
            }
        val response =
            restTemplate.postForEntity<TestObject>(localhost("/api/test"), HttpEntity(json, jsonHeaders))
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!).isEqualTo(json)
    }

    @Test
    fun `skal feile hvis påkrevd booleanfelt er null`() {
        val entity =
            HttpEntity(
                "{}",
                HttpHeaders().apply {
                    contentType = APPLICATION_JSON
                    accept = listOf(APPLICATION_JSON)
                    setBearerAuth(lokalTestToken)
                },
            )

        val exception =
            catchProblemDetailException {
                restTemplate.postForEntity<TestObjectBoolean>(localhost("/api/test/boolean"), entity)
            }
        assertThat(exception.detail.detail).contains("Missing required creator property 'verdi'")
    }

    @Test
    fun `skal håndtere ukjent feil`() {
        var response =
            catchException {
                restTemplate.exchange<String>(
                    localhost("/api/test/error"),
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                )
            }
        assertThat(response).isInstanceOf(InternalServerError::class.java)
        assertInternalServerError(response as InternalServerError)
    }

    @Test
    fun `skal håndtere ukjent feil med forventet responstype med exchange`() {
        val response =
            catchException {
                restTemplate.exchange<TestObject>(
                    localhost("/api/test/error"),
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                )
            }
        assertThat(response).isInstanceOf(InternalServerError::class.java)
        assertInternalServerError(response as InternalServerError)
    }

    private fun assertInternalServerError(response: InternalServerError) {
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.responseHeaders?.contentType).isEqualTo(APPLICATION_PROBLEM_JSON)
        assertThat(response.responseBodyAsString).isEqualTo(feilJson)
    }
}

@RestController
@RequestMapping("/api/test")
@Unprotected
class TestApiController {
    @GetMapping
    fun get(): TestObject =
        TestObject(
            tekst = "abc",
            dato = LocalDate.of(2023, 1, 1),
            tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
        )

    @PostMapping
    fun post(
        @RequestBody testObject: TestObject,
    ): TestObject = testObject

    @GetMapping("error")
    fun error() {
        error("error")
    }

    @GetMapping("azuread")
    @ProtectedWithClaims(issuer = "azuread")
    fun getMedAzureAd(): TestObject = get()
}

@RestController
@RequestMapping("/api/test")
@Unprotected
class TestBooleanController {
    @ExceptionHandler(HttpMessageConversionException::class)
    fun handleThrowable(throwable: HttpMessageConversionException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, throwable.message)

    @PostMapping("/boolean")
    fun boolean(
        @RequestBody testObject: TestObjectBoolean,
    ): TestObjectBoolean = testObject
}

data class TestObject(
    val tekst: String,
    val dato: LocalDate,
    val tidspunkt: LocalDateTime,
)

data class TestObjectBoolean(
    val verdi: Boolean,
)
