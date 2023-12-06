package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.InMemoryProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.InvalidDefinitionException

@Service
class DefinitionService(
  private val repository: InMemoryProductDefinitionRepository,
  private val dataRepository: ConfiguredApiRepository,
) {
  val mapper: ReportDefinitionMapper = ReportDefinitionMapper(FakeConfiguredApiService(repository, dataRepository))

  fun validateAndSave(
    definition: ProductDefinition,
    caseLoads: List<String>,
  ) {
    try {
      // Attempt mapping to assert references are correct
      mapper.map(definition, RenderMethod.HTML, 1, caseLoads)

      // Check each query executes successfully
      definition.dataset.forEach { dataset ->
        dataRepository.count(
          reportId = definition.id,
          filters = emptyList(),
          caseloadFields = dataset.schema.field.filter { it.caseload }.map { it.name },
          query = dataset.query,
          userCaseloads = caseLoads,
        )
      }
    } catch (e: Exception) {
      throw InvalidDefinitionException(e)
    }
    repository.save(definition)
  }

  fun deleteById(definitionId: String) {
    repository.deleteById(definitionId)
  }
}
