package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config.ErrorResponse


class ReportDefinitionIntegrationTest: IntegrationTestBase() {

  @Autowired
  lateinit var gson: Gson

  val productDefinition =
    """
      {
      "id": "1",
      "name": "2",
      "description": "3",
      "metadata": {
        "author": "4",
        "version": "5",
        "owner": "6",
        "purpose": "7",
        "profile": "8",
        "dqri": "9"
      },
      "datasource": [
        {
          "id": "10",
          "name": "11"
        }
      ],
      "dataset": [
        {
          "id": "20",
          "name": "21",
          "query": "SELECT '70' AS F30",
          "schema": {
          "field": [
            {
              "name": "F30",
              "type": "string",
              "caseload": false
            }
          ]
        }
      }
      ],
      "report": [
        {
          "id": "40",
          "name": "41",
          "description": "42",
          "created": "2023-12-06",
          "version": "43",
          "dataset": "20",
          "policy": [
            "60"
          ],
          "render": "HTML",
          "specification": {
            "template": "list",
            "field": [
              {
                "name": "F30",
                "display": "51",
                "wordWrap": "None",
                "filter": {
                  "type": "Radio",
                  "dynamicoptions": {
                    "minimumLength": 2,
                    "returnAsStaticOptions": true
                   }
                },
                "sortable": true,
                "defaultsort": false,
                "formula": "52",
                "visible": true
              }
            ]
          },
          "destination": []
        }
      ],
      "policy": [
        {
          "id": "60",
          "type": "61",
          "rule": []
        }
      ]
    }
    """.trimIndent()

  @Test
  fun `Valid definition is saved and is presented by list endpoint`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk

    val body = webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody

    val result = gson.fromJson<List<ReportDefinition>>(body, object : TypeToken<ArrayList<ReportDefinition?>?>() {}.getType())

    assertThat(result).isNotNull
    assertThat(result).hasSize(1)
    assertThat(result).first().isNotNull

    val definition = result!!.first()

    assertThat(definition.name).isEqualTo("2")
    assertThat(definition.description).isEqualTo("3")
    assertThat(definition.variants).hasSize(1)
    assertThat(definition.variants[0]).isNotNull

    assertVariant(definition.variants[0])
  }

  @Test
  fun `Valid definition is saved and is presented by definition endpoint`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    val body = webTestClient.get()
      .uri("/definitions/1/40")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody

    val result = gson.fromJson(body, SingleVariantReportDefinition::class.java)

    assertThat(result.name).isEqualTo("2")
    assertThat(result.description).isEqualTo("3")
    assertThat(result.variant).isNotNull

    assertVariant(result.variant)
  }

  private fun assertVariant(variant: VariantDefinition) {
    assertThat(variant.id).isEqualTo("40")
    assertThat(variant.resourceName).isEqualTo("reports/1/40")
    assertThat(variant.name).isEqualTo("41")
    assertThat(variant.description).isEqualTo("42")
    assertThat(variant.specification).isNotNull
    assertThat(variant.specification?.fields).hasSize(1)

    val field = variant.specification!!.fields[0]

    assertThat(field.name).isEqualTo("F30")
    assertThat(field.display).isEqualTo("51")
    assertThat(field.sortable).isEqualTo(true)
    assertThat(field.defaultsort).isEqualTo(false)
    assertThat(field.type).isEqualTo(FieldType.String)
    assertThat(field.wordWrap).isEqualTo(uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.WordWrap.None)
    assertThat(field.filter).isNotNull()
    assertThat(field.filter!!.type).isEqualTo(uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType.Radio)
    assertThat(field.filter!!.staticOptions).hasSize(1)
  }

  @Test
  fun `Empty definition is rejected`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{}")
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `Definition with invalid dataset reference is rejected`() {
    webTestClient.put()
      .uri("/definitions/people")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
          {
            "id": "people",
            "name": "People",
            "description": "Reports about people",
            "metadata": {
              "author": "Stu",
              "owner": "Stu",
              "version": "1.0.0"
            },
            "datasource": [
              {
                "id": "redshift",
                "name": "redshift"
              }
            ],
            "dataset": [
              {
                "id": "people",
                "name": "All",
                "datasource": "redshift",
                "query": "SELECT prisoners.number AS prisonNumber, prisoners.lastname, prisoners.firstname FROM datamart.domain.prisoner_prisoner AS prisoners",
                "schema": {
                  "field": []
                }
              }
            ],
            "policy": [],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04",
                "classification": "OFFICIAL",
                "version": "1.2.3",
                "render": "HTML",
                "dataset": "invalid",
                "specification": {
                  "template": "list",
                  "field": []
                }
              }
            ]
          }

        """.trimIndent(),
      )
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `Definition with invalid field reference is rejected`() {
    webTestClient.put()
      .uri("/definitions/people")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
          {
            "id": "people",
            "name": "People",
            "description": "Reports about people",
            "metadata": {
              "author": "Stu",
              "owner": "Stu",
              "version": "1.0.0"
            },
            "datasource": [
              {
                "id": "redshift",
                "name": "redshift"
              }
            ],
            "dataset": [
              {
                "id": "people",
                "name": "All",
                "datasource": "redshift",
                "query": "SELECT prisoners.number AS prisonNumber, prisoners.lastname, prisoners.firstname FROM datamart.domain.prisoner_prisoner AS prisoners",
                "schema": {
                  "field": []
                }
              }
            ],
            "policy": [],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04",
                "classification": "OFFICIAL",
                "version": "1.2.3",
                "render": "HTML",
                "dataset": "people",
                "specification": {
                  "template": "list",
                  "field": [
                  {
                    "name": "prisonNumber",
                    "display": "Prison Number",
                    "formula": "",
                    "visible": true,
                    "sortable": true,
                    "defaultsort": true
                  }
                ]
                }
              }
            ]
          }

        """.trimIndent(),
      )
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `Definition with invalid SQL query is rejected`() {
    val result = webTestClient.put()
      .uri("/definitions/people")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
          {
            "id": "people",
            "name": "People",
            "description": "Reports about people",
            "metadata": {
              "author": "Stu",
              "owner": "Stu",
              "version": "1.0.0"
            },
            "datasource": [
              {
                "id": "redshift",
                "name": "redshift"
              }
            ],
            "dataset": [
              {
                "id": "people",
                "name": "All",
                "datasource": "redshift",
                "query": "SELECT cheese, FROM tables",
                "schema": {
                  "field": [
                    {
                      "name": "prisonNumber",
                      "type": "string",
                      "display": ""
                    }
                  ]
                }
              }
            ],
            "policy": [],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04",
                "classification": "OFFICIAL",
                "version": "1.2.3",
                "render": "HTML",
                "dataset": "people",
                "specification": {
                  "template": "list",
                  "field": [
                    {
                      "name": "prisonNumber",
                      "display": "Prison Number",
                      "formula": "",
                      "visible": true,
                      "sortable": true,
                      "defaultsort": true
                    }
                  ]
                }
              }
            ]
          }

        """.trimIndent(),
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult<ErrorResponse>()
      .responseBody
      .blockFirst()

    assertThat(result!!.developerMessage).contains("bad SQL grammar")
  }

  @Test
  fun `Property with GSON deserialisation annotations is deserialised correctly`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk

    val body = webTestClient.get()
      .uri("/definitions/1/40")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody

    val result = gson.fromJson(body, SingleVariantReportDefinition::class.java)

    val field = result!!.variant.specification!!.fields[0]

    assertThat(field.defaultsort).isFalse()
    assertThat(field.filter!!.dynamicOptions!!.returnAsStaticOptions).isTrue()
    assertThat(field.filter!!.staticOptions!![0].name).isEqualTo("70")
  }

  @Test
  fun `Definition is deleted`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk

    webTestClient.delete()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk

    val result = webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(0)
  }
}
