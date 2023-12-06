package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.InMemoryProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.InvalidDefinitionException

@Service
class DefinitionService(
  val repository: InMemoryProductDefinitionRepository,
  val mapper: ReportDefinitionMapper,
) {

  fun validateAndSave(
    definition: ProductDefinition,
  ) {
    try {
      mapper.map(definition, RenderMethod.HTML)
    } catch (e: IllegalArgumentException) {
      throw InvalidDefinitionException(e)
    }
    repository.save(definition)
  }

  fun deleteById(definitionId: String) {
    repository.deleteById(definitionId)
  }
}
