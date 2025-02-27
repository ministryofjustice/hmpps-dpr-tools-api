package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AbstractProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.DefinitionNotFoundException
import java.util.concurrent.ConcurrentHashMap

@Service
class InMemoryProductDefinitionRepository(identifiedHelper: IdentifiedHelper) : AbstractProductDefinitionRepository(identifiedHelper) {

  private val definitions: ConcurrentHashMap<String, Pair<ProductDefinition, String>> = ConcurrentHashMap()

  override fun getProductDefinitions(path: String?): List<ProductDefinition> = definitions.values.map { it.first }.toList()

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition = definitions.getOrElse(definitionId) { throw DefinitionNotFoundException("Invalid report id provided: $definitionId") }.first

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String?): SingleReportProductDefinition {
    try {
      return super.getSingleReportProductDefinition(definitionId, reportId, null)
    } catch (e: ValidationException) {
      throw DefinitionNotFoundException(e.message)
    }
  }

  fun save(definition: ProductDefinition, originalBody: String) {
    definitions[definition.id] = Pair(definition, originalBody)
  }

  fun deleteById(definitionId: String) {
    definitions.remove(definitionId)
  }

  fun getOriginalBody(definitionId: String) = definitions[definitionId]?.second
}
