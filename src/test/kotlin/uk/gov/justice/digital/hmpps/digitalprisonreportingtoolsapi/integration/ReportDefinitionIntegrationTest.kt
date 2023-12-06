package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StaticFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.WordWrap
import java.time.LocalDate

class ReportDefinitionIntegrationTest : IntegrationTestBase() {

  val productDefinition = ProductDefinition(
    id = "1",
    name = "2",
    description = "3",
    metadata = MetaData(
      author = "4",
      version = "5",
      owner = "6",
      purpose = "7",
      profile = "8",
      dqri = "9",
    ),
    datasource = listOf(
      Datasource(
        id = "10",
        name = "11",
      ),
    ),
    dataset = listOf(
      Dataset(
        id = "20",
        name = "21",
        query = "22",
        schema = Schema(
          field = listOf(
            SchemaField(
              name = "30",
              type = ParameterType.String,
              caseload = false,
            ),
          ),
        ),
      ),
    ),
    report = listOf(
      Report(
        id = "40",
        name = "41",
        description = "42",
        created = LocalDate.now(),
        dataset = "\$ref:20",
        policy = listOf("\$ref:60"),
        render = RenderMethod.HTML,
        version = "43",
        specification = Specification(
          template = "list",
          field = listOf(
            ReportField(
              name = "30",
              display = "51",
              wordWrap = WordWrap.None,
              filter = FilterDefinition(type = FilterType.Radio, listOf(StaticFilterOption("70", "71"))),
              sortable = true,
              defaultSort = false,
              formula = "52",
              visible = true,
            ),
          ),
        ),
      ),
    ),
    policy = listOf(
      Policy(
        id = "60",
        type = "61",
        rule = emptyList(),
      ),
    ),
  )

  @Test
  fun `Valid definition is saved and is presented by list endpoint`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
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
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(1)
    assertThat(result.responseBody).first().isNotNull

    val definition = result.responseBody!!.first()

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

    assertThat(field.name).isEqualTo("30")
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
            "policy": [],
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
  fun `Definition is deleted`() {
    webTestClient.put()
      .uri("/definitions/1")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
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
