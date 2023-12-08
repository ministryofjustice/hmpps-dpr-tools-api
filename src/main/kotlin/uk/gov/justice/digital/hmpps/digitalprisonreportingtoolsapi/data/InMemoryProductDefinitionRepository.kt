package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AbstractProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.DefinitionNotFoundException
import java.util.concurrent.ConcurrentHashMap

@Service
class InMemoryProductDefinitionRepository : AbstractProductDefinitionRepository() {

  private val definitions: ConcurrentHashMap<String, ProductDefinition> = ConcurrentHashMap()

  override fun getProductDefinitions(): List<ProductDefinition> {
    return definitions.values.toList()
  }

  override fun getProductDefinition(definitionId: String): ProductDefinition =
    definitions.getOrElse(definitionId) { throw DefinitionNotFoundException("Invalid report id provided: $definitionId") }

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition {
    try {
      return super.getSingleReportProductDefinition(definitionId, reportId)
    } catch (e: ValidationException) {
      throw DefinitionNotFoundException(e.message)
    }
  }

  fun save(definition: ProductDefinition) {
    definitions[definition.id] = definition
  }

  fun deleteById(definitionId: String) {
    definitions.remove(definitionId)
  }
}
