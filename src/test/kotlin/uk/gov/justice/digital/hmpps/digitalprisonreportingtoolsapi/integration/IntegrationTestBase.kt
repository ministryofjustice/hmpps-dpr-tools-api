package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {

  @Value("\${dpr.lib.user.role}")
  lateinit var authorisedRole: String

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  companion object {

    lateinit var wireMockServer: WireMockServer

    @BeforeAll
    @JvmStatic
    fun setupClass() {
      wireMockServer = WireMockServer(
        WireMockConfiguration.wireMockConfig().port(9999),
      )
      wireMockServer.start()
    }
  }

  internal fun setAuthorisation(
    user: String = "request-user",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(
    clientId = "hmpps-digital-tools-api",
    user,
    scopes,
    roles,
  )

  @BeforeEach
  fun setup() {
    wireMockServer.resetAll()
    stubAccessTokenResponse()
    stubPrisonerCaseloadResponse()
    stubPrisonerInfoResponse()
    stubUserRolesResponse()
  }

  protected fun stubAccessTokenResponse() {
    wireMockServer.stubFor(
      WireMock.post("/auth/oauth/token")
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(
              """
              {
                "access_token": "${setAuthorisation(roles = listOf(authorisedRole))}",
                 "expires_in": 3599,
                 "token_type": "Bearer",
                 "scope": "read",
                 "sub": "request-user",
                 "user_name": "request-user",
                 "auth_source": "none"
               }
              """.trimIndent()
            ),
        ),
    )
  }

  protected fun stubPrisonerCaseloadResponse() {
   wireMockServer.stubFor(
      WireMock.get("/prisonusers/request-user/caseloads")
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(
              """
              {
                "username":"request-user",
                "active":"true",
                "accountType":"GENERAL",
                "activeCaseload": {"id":"LWSTMC","name":"Lowestoft (North East Suffolk) Magistrat"},
                "caseloads":[
                  {"id":"LWSTMC","name":"Lowestoft (North East Suffolk) Magistrat"}
                ]
              }
              """.trimIndent()
            ),
        ),
    )
  }

  protected fun stubPrisonerInfoResponse() {
    wireMockServer.stubFor(
      WireMock.get("/users/request-user")
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(
              """
              {
                "username":"request-user",
                "active":true,
                "name":"request-user",
                "authSource":"${AuthSource.NONE.name}",
                "userId":"abc123",
                "uuid":"989q-2f3f-2g3-g34"
              }
              """.trimIndent()
            ),
        ),
    )
  }

  protected fun stubUserRolesResponse() {
    wireMockServer.stubFor(
      WireMock.get("/users/request-user/roles")
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(
              """
               [
                {"roleCode":"$authorisedRole"}
               ]
              """.trimIndent()
            ),
        ),
    )
  }
}
