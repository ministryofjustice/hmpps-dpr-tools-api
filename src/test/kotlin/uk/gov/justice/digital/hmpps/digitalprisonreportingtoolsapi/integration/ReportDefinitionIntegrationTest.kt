package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.*
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config.ErrorResponse

class ReportDefinitionIntegrationTest : IntegrationTestBase() {

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
          "created": "2023-12-07T09:21:00.000Z",
          "version": "43",
          "dataset": "20",
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
          "type": "row-level",
          "action": ["TRUE"],
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

    val result = webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinitionSummary>()
      .returnResult()
      .responseBody!!

    assertThat(result).hasSizeGreaterThan(0)
    val definition = result.findLast { it.id == "1" }!!

    assertThat(definition.name).isEqualTo("2")
    assertThat(definition.description).isEqualTo("3")
    assertThat(definition.variants).hasSize(1)
    assertThat(definition.variants[0]).isNotNull

    assertThat(definition.variants[0].id).isEqualTo("40")
    assertThat(definition.variants[0].name).isEqualTo("41")
    assertThat(definition.variants[0].description).isEqualTo("42")
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

    val result = webTestClient.get()
      .uri("/definitions/1/40")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(SingleVariantReportDefinition::class.java)
      .returnResult()
      .responseBody!!

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
    assertThat(field.wordWrap).isEqualTo(WordWrap.None)
    assertThat(field.filter).isNotNull()
    assertThat(field.filter!!.type).isEqualTo(FilterType.Radio)
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
            "policy": [
              {
                "id": "60",
                "type": "row-level",
                "action": ["TRUE"],
                "rule": []
              }
            ],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04T14:41:00.000Z",
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
            "policy": [
              {
                "id": "60",
                "type": "row-level",
                "action": ["TRUE"],
                "rule": []
              }
            ],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04T14:41:00.000Z",
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
            "policy": [
              {
                "id": "60",
                "type": "row-level",
                "action": ["TRUE"],
                "rule": []
              }
            ],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04T14:41:00.000Z",
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
  fun `Definition with date range filter is returned correctly`() {
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
                "query": "SELECT 1",
                "schema": {
                  "field": [
                    {
                      "name": "date",
                      "type": "date",
                      "display": ""
                    }
                  ]
                }
              }
            ],
            "policy": [
              {
                "id": "60",
                "type": "row-level",
                "action": ["TRUE"],
                "rule": []
              }
            ],
            "report": [
              {
                "id": "everyone",
                "name": "Everyone",
                "description": "EVERYONE",
                "created": "2023-12-04T14:41:00.000Z",
                "classification": "OFFICIAL",
                "version": "1.2.3",
                "render": "HTML",
                "dataset": "people",
                "specification": {
                  "template": "list",
                  "field": [
                    {
                      "name": "date",
                      "display": "Date",
                      "formula": "",
                      "visible": true,
                      "sortable": true,
                      "defaultsort": true,
                      "filter" : {
                          "type": "daterange"
                      }
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
      .isOk

    val body = webTestClient.get()
      .uri("/definitions/people/everyone")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody

    assertThat(body).contains("\"type\":\"date\"")
    assertThat(body).contains("\"type\":\"daterange\"")
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

    val result = webTestClient.get()
      .uri("/definitions/1/40")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(SingleVariantReportDefinition::class.java)
      .returnResult()
      .responseBody

    val field = result!!.variant.specification!!.fields[0]

    assertThat(field.defaultsort).isFalse()
    assertThat(field.filter).isNotNull()
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

    webTestClient.get()
      .uri("/definitions/1/40")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
