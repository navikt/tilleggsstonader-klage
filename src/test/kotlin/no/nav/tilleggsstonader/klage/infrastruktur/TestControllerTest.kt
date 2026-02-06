package no.nav.tilleggsstonader.klage.infrastruktur

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.klage.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.test.web.servlet.client.expectBody
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

class TestControllerTest : IntegrationTest() {
    val json = """{"tekst":"abc","dato":"2023-01-01","tidspunkt":"2023-01-01T12:00:03"}"""
    val feilJson =
        """{"type":"about:blank","title":"Internal Server Error","status":500,"detail":"Ukjent feil","instance":"/api/test/error"}"""

    lateinit var jsonHeaders: HttpHeaders

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        jsonHeaders =
            HttpHeaders().apply {
                contentType = APPLICATION_JSON
                accept = listOf(APPLICATION_JSON)
                setBearerAuth(lokalTestToken)
            }
    }

    @Test
    fun `skal kunne hente json fra endepunkt`() {
        val response =
            restTestClient
                .get()
                .uri("/api/test")
                .headers { it.addAll(headers) }
                .exchangeSuccessfully()
                .expectBody<String>()
                .returnResult()

        assertThat(response.responseBody).isEqualTo(json)
    }

    @Test
    fun `skal kunne sende inn object`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )

        val response =
            restTestClient
                .post()
                .uri("/api/test")
                .body(json)
                .headers { it.addAll(headers) }
                .exchangeSuccessfully()
                .expectBody<TestObject>()
                .returnResult()

        assertThat(response.responseBody).isEqualTo(json)
    }

    @Test
    fun `skal kunne sende inn object med json header`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )

        val response =
            restTestClient
                .post()
                .uri(localhost("/api/test"))
                .body(json)
                .headers { it.addAll(jsonHeaders) }
                .exchangeSuccessfully()
                .expectBody<TestObject>()
                .returnResult()

        assertThat(response.responseBody).isEqualTo(json)
    }

    @Test
    fun `skal feile hvis påkrevd booleanfelt er null`() {
        restTestClient
            .post()
            .uri(localhost("/api/test/boolean"))
            .body("{}")
            .headers { it.addAll(jsonHeaders) }
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("JSON parse error: Missing required creator property 'verdi' (index 0)")
    }

    @Test
    fun `skal håndtere ukjent feil`() {
        val problemDetail: Map<String, Any> =
            restTestClient
                .get()
                .uri("/api/test/error")
                .headers { it.addAll(headers) }
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectHeader()
                .contentType(APPLICATION_PROBLEM_JSON)
                .expectBody<Map<String, Any>>()
                .returnResult()
                .responseBody!!

        assertThat(problemDetail["status"]).isEqualTo(500)
        assertThat(problemDetail["title"]).isEqualTo("Internal Server Error")
        assertThat(problemDetail["detail"]).isEqualTo("Ukjent feil")
        assertThat(problemDetail["instance"]).isEqualTo("/api/test/error")
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
