package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.WordWrap
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.serialization.SerialisationService

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
        "profile": "8"
      },
      "datasource": [
        {
          "id": "10",
          "name": "activities"
        }
      ],
      "dataset": [
        {
          "id": "20",
          "name": "21",
          "query": "SELECT '70' AS F30",
          "datasource": "10",
          "schema": {
            "field": [
              {
                "name": "F30",
                "type": "string"
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
          "version": "43",
          "dataset": "20",
          "render": "HTML",
          "specification": {
            "template": "list",
            "field": [
              {
                "name": "${'$'}ref:F30",
                "display": "51",
                "wordwrap": "none",
                "filter": {
                  "type": "radio",
                  "dynamicoptions": {
                    "minimumLength": 2,
                    "returnAsStaticOptions": true
                   }
                },
                "sortable": true,
                "defaultsort": false,
                "visible": "true"
              }
            ]
          }
        }
      ],
      "policy": [
      {
        "id": "accessPolicy",
        "type": "access",
        "rule": [
          {
            "effect": "permit",
            "condition": []
          }
        ]
        },
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
      .uri("/definitions")
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
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinitionSummary>()
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
      .uri("/definitions")
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
      .uri("/definitions")
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
                "created": "2023-12-04T14:41:00",
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
      .uri("/definitions")
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
                "created": "2023-12-04T14:41:00",
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
                    "visible": "true",
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
  fun `Definition with date range filter is returned correctly`() {
    webTestClient.put()
      .uri("/definitions")
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
                "id": "accessPolicy",
                "type": "access",
                "rule": [
                  {
                    "effect": "permit",
                    "condition": []
                  }
                ]
              },
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
                "classification": "OFFICIAL",
                "version": "1.2.3",
                "render": "HTML",
                "dataset": "people",
                "specification": {
                  "template": "list",
                  "section": [],
                  "field": [
                    {
                      "name": "date",
                      "display": "Date",
                      "formula": "",
                      "visible": "true",
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
  fun `Invalid definition returns kotlinx-serialization exception properly`() {
    val json = """
      {
        "id": "id1",
        "name": "p1",
        "description": "d1",
        "metadata": {
          "author": "DPR",
          "owner": "DPR",
          "version": "0.1"
        },
        "datasource": [
          {
            "id": "ds1",
            "name": "dsn1"
          }
        ],
        "dataset": [
          {
            "id": "dsi1",
            "name": "dsin1",
            "datasource": "ds1",
            "query": "select 1",
            "schema": {
              "field": [
                {
                  "name": "n1",
                  "type": "string",
                  "display": "n1"
                },
                {
                  "name": "n2",
                  "type": "int",
                  "display": "n2"
                },
                {
                  "name": "n3",
                  "type": "int",
                  "display": "n3"
                },
                {
                  "name": "n4",
                  "type": "int",
                  "display": "n4"
                }
              ]
            }
          },
          {
            "id": "ds2",
            "name": "dsn2",
            "datasource": "ds2",
            "query": "select 2",
            "schema": {
              "field": [
                {
                  "name": "sf1",
                  "type": "timestamp",
                  "display": "sf1d",
                  "filter": {
                    "index": 0,
                    "interactive": "true",
                    "type": "daterange",
                    "default": "today(-3, months) - today()"
                  }
                },
                {
                  "name": "sf2",
                  "type": "int",
                  "display": "sf2",
                  "formula": "make_url('/dpr/request-report/report/rep1/repvar2/filters?',${'$'}{sf3},TRUE)"
                },
                {
                  "name": "sf3",
                  "type": "int",
                  "display": "sf3"
                },
                {
                  "name": "sf4",
                  "type": "int",
                  "display": "sf4"
                },
                {
                  "name": "sf5",
                  "type": "int",
                  "display": "sf5"
                },
                {
                  "name": "sf6",
                  "type": "int",
                  "display": "sf6"
                },
                {
                  "name": "sf7",
                  "type": "int",
                  "display": "sf7"
                }
              ]
            }
          },
          {
            "id": "ds3",
            "name": "dsn3",
            "datasource": "ds3",
            "query": "select 3",
            "schema": {
              "field": [
                {
                  "name": "sf1",
                  "type": "string",
                  "display": "sf1"
                }
              ]
            }
          }
        ],
        "policy": [
          {
            "id": "lao",
            "type": "lao",
            "rule": [
              {
                "effect": "permit",
                "condition": []
              }
            ]
          },
          {
            "id": "access",
            "type": "access",
            "rule": [
              {
                "effect": "permit",
                "condition": [
                  {
                    "match": ["${'$'}{role}", "SOME_ROLE"]
                  }
                ]
              }
            ]
          }
        ],
        "report": [
          {
            "id": "rep1",
            "name": "rep1",
            "description": "rep1",
            "classification": "Official",
            "version": "1.0.0",
            "render": "HTML",
            "dataset": "${'$'}ref:ds1",
            "feature": [
              {
                "type": "print"
              }
            ],
            "metadata": {
              "hints": ["interactive"]
            },
            "specification": {
              "template": "list",
              "field": [
                {
                  "name": "${'$'}ref:sf1",
                  "formula": "",
                  "visible": "true",
                  "sortable": true,
                  "defaultsort": true
                },
                {
                  "name": "${'$'}ref:sf2",
                  "display": "Created",
                  "formula": "",
                  "visible": "true",
                  "sortable": true,
                  "defaultsort": false,
                  "filter": {
                    "index": 1,
                    "type": "daterange"
                  }
                },
                {
                  "name": "${'$'}ref:sf3",
                  "display": "Status",
                  "visible": "true",
                  "sortable": false,
                  "defaultsort": false,
                  "filter": {
                    "index": 1,
                    "type": "multiselect",
                    "interactive": "true",
                    "staticoptions": [
                      {
                        "name": "DRAFT",
                        "display": "Draft"
                      },
                      {
                        "name": "AGREED",
                        "display": "Agreed"
                      },
                      {
                        "name": "DO_NOT_AGREE",
                        "display": "Do not agree"
                      },
                      {
                        "name": "COULD_NOT_ANSWER",
                        "display": "Could not answer"
                      },
                      {
                        "name": "UPDATED_COULD_NOT_AGREE",
                        "display": "Updated Not Agree"
                      },
                      {
                        "name": "UPDATED_AGREED",
                        "display": "Updated Agreed"
                      }
                    ]
                  }
                }
              ]
            }
          }
        ],
        "dashboard": [
          {
            "id": "d1",
            "name": "d1",
            "description": "d1",
            "dataset": "ds2",
            "section": [
              {
                "id": "s1",
                "display": "s1",
                "visualisation": [
                  {
                    "id": "pc1",
                    "type": "list",
                    "display": "v1",
                    "option": {
                      "showLatest": false
                    },
                    "column": {
                      
                      "measure": [
                        {
                          "id": "${'$'}ref:ts",
                          "display": "Date"
                        },
                        {
                          "id": "${'$'}ref:link",
                          "display": "Count"
                        },
                        {
                          "id": "${'$'}ref:draft_count",
                          "display": "# No agreement"
                        },
                        {
                          "id": "${'$'}ref:agreed_pct",
                          "display": "Agreed %"
                        },
                        {
                          "id": "${'$'}ref:not_agreed_pct",
                          "display": "Not agreed %"
                        },
                        {
                          "id": "${'$'}ref:could_not_answer_pct",
                          "display": "Could not answer %"
                        },
                        {
                          "id": "${'$'}ref:updated_agreed_count_pct",
                          "display": "Updated Agreed %"
                        },
                        {
                          "id": "${'$'}ref:updated_not_agreed_count_pct",
                          "display": "Updated count not agree %"
                        },
                        {
                          "id": "${'$'}ref:total_to_date",
                          "display": "Total to date"
                        }
                      ],
                      "expectNull": false
                    }
                  }
                ]
              }
            ]
          }
        ]
      }
    """.trimIndent()
      .replace("\"effect\": \"permit\"", "\"effect\": \"zxcxzc\"")
      .replace("\"sortable\": true", "\"sortable\": null")
      .replace("\"name\": \"DO_NOT_AGREE\",", "")
      .replace("\"datasource\": \"ds3\",", "")
    val body = webTestClient.put()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(json)
      .exchange()
      .expectBodyList(SerialisationService.DPDIssue::class.java)
      .returnResult()
      .responseBody

    assertThat(body).containsExactlyInAnyOrder(
      SerialisationService.DPDIssue("\$.dataset[2].datasource", SerialisationService.DPDIssueType.MISSING_FIELD, null),
      SerialisationService.DPDIssue("\$.policy[0].rule[0].effect", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, "Potential values are: [permit, deny]"),
      SerialisationService.DPDIssue("\$.policy[1].rule[0].effect", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, "Potential values are: [permit, deny]"),
      SerialisationService.DPDIssue("\$.report[0].specification.field[0].sortable", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, ".....isible\":\"true\",\"sortable\":null,\"defaultsort\":true},{\"name\":\"....."),
      SerialisationService.DPDIssue("\$.report[0].specification.field[1].sortable", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, ".....isible\":\"true\",\"sortable\":null,\"defaultsort\":false,\"filter\":....."),
      SerialisationService.DPDIssue("\$.report[0].specification.field[2].filter.staticoptions[2].name", SerialisationService.DPDIssueType.MISSING_FIELD, null),
    )
  }

  @Test
  fun `Property with GSON deserialisation annotations is deserialised correctly`() {
    webTestClient.put()
      .uri("/definitions")
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
    assertThat(field.filter!!.staticOptions!![0].name).isEqualTo("70")
  }

  @Test
  fun `Definition is deleted`() {
    webTestClient.put()
      .uri("/definitions")
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

  @Test
  fun `Definition is fetched`() {
    webTestClient.put()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(productDefinition)
      .exchange()
      .expectStatus()
      .isOk

    val savedDefinition = webTestClient.get()
      .uri("/definitions/original/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody

    assertThat(savedDefinition).isEqualTo(productDefinition)
  }
}
