package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionKotlinxConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.serialization.SerialisationService

class SerialisationServiceTest {
  private val kotlinSerializationConfig: DefinitionKotlinxConfig = DefinitionKotlinxConfig()
  private val serializationService: SerialisationService = SerialisationService(kotlinSerializationConfig.kotlinxJson())

  @Test
  fun test() {
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

    val testjson = Json {
      prettyPrint = true
      allowComments = false
      allowTrailingComma = false
      allowStructuredMapKeys = false
      allowSpecialFloatingPointValues = false
      coerceInputValues = false
      encodeDefaults = true
      explicitNulls = true
      ignoreUnknownKeys = false
      isLenient = false
    }
    val resp = serializationService.deserializeProductDefinition(json)
    println(testjson.encodeToString(resp.second))
    assertThat(resp.second).containsExactlyInAnyOrder(
      SerialisationService.DPDIssue("\$.dataset[2].datasource", SerialisationService.DPDIssueType.MISSING_FIELD, null),
      SerialisationService.DPDIssue("\$.policy[0].rule[0].effect", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, "Potential values are: [permit, deny]"),
      SerialisationService.DPDIssue("\$.policy[1].rule[0].effect", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, "Potential values are: [permit, deny]"),
      SerialisationService.DPDIssue("\$.report[0].specification.field[0].sortable", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, ".....isible\":\"true\",\"sortable\":null,\"defaultsort\":true},{\"name\":\"....."),
      SerialisationService.DPDIssue("\$.report[0].specification.field[1].sortable", SerialisationService.DPDIssueType.INVALID_FIELD_VALUE, ".....isible\":\"true\",\"sortable\":null,\"defaultsort\":false,\"filter\":....."),
      SerialisationService.DPDIssue("\$.report[0].specification.field[2].filter.staticoptions[2].name", SerialisationService.DPDIssueType.MISSING_FIELD, null),
    )
  }
}
