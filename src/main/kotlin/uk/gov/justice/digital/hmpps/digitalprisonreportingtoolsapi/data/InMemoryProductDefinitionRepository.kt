package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition

@Service
class InMemoryProductDefinitionRepository : ProductDefinitionRepository {

  companion object {
    private const val schemaRefPrefix = "\$ref:"
  }

  private val definitions: MutableMap<String, ProductDefinition> = HashMap()

  override fun getProductDefinitions(): List<ProductDefinition> {
    return definitions.values.toList()
  }

  override fun getProductDefinition(definitionId: String): ProductDefinition =
    definitions.getOrElse(definitionId) { throw ValidationException("Invalid report id provided: $definitionId") }

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition {
    val productDefinition = getProductDefinition(definitionId)
    val reportDefinition = productDefinition.report
      .filter { it.id == reportId }
      .ifEmpty { throw ValidationException("Invalid report variant id provided: $reportId") }
      .first()

    val dataSetId = reportDefinition.dataset.removePrefix(schemaRefPrefix)
    val dataSet = productDefinition.dataset
      .filter { it.id == dataSetId }
      .ifEmpty { throw ValidationException("Invalid dataSetId in report: $dataSetId") }
      .first()

    return SingleReportProductDefinition(
      id = definitionId,
      name = productDefinition.name,
      description = productDefinition.description,
      metadata = productDefinition.metadata,
      datasource = productDefinition.datasource.first(),
      dataset = dataSet,
      report = reportDefinition,
    )
  }

  fun save(definition: ProductDefinition) {
    definitions[definition.id] = definition
  }

  fun deleteById(definitionId: String) {
    definitions.remove(definitionId)
  }
}
