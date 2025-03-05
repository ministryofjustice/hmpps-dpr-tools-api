package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

interface CrudProductDefinitionRepository : ProductDefinitionRepository {

  fun save(definition: ProductDefinition, originalBody: String)

  fun deleteById(definitionId: String)

  fun getOriginalBody(definitionId: String): String?
}
