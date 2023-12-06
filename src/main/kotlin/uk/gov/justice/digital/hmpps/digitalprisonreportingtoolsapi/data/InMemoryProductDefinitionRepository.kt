package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AbstractProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import java.util.concurrent.ConcurrentHashMap

@Service
class InMemoryProductDefinitionRepository : AbstractProductDefinitionRepository() {

  private val definitions: ConcurrentHashMap<String, ProductDefinition> = ConcurrentHashMap()

  override fun getProductDefinitions(): List<ProductDefinition> {
    return definitions.values.toList()
  }

  override fun getProductDefinition(definitionId: String): ProductDefinition =
    definitions.getOrElse(definitionId) { throw ValidationException("Invalid report id provided: $definitionId") }

  fun save(definition: ProductDefinition) {
    definitions[definition.id] = definition
  }

  fun deleteById(definitionId: String) {
    definitions.remove(definitionId)
  }
}
