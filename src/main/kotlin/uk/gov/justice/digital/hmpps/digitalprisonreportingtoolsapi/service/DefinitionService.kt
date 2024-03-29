package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
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
    authenticationToken: DprAuthAwareAuthenticationToken,
    originalBody: String,
  ) {
    try {
      // Attempt mapping to assert references are correct
      mapper.map(definition, RenderMethod.HTML, authenticationToken)

      // Check each query executes successfully
      definition.dataset.forEach { dataset ->
        dataRepository.count(
          reportId = definition.id,
          filters = emptyList(),
          query = dataset.query,
          policyEngineResult = "FALSE",
          dataSourceName = definition.datasource.first().name,
        )
      }
    } catch (e: Exception) {
      throw InvalidDefinitionException(e)
    }
    repository.save(definition, originalBody)
  }

  fun deleteById(definitionId: String) {
    repository.deleteById(definitionId)
  }

  fun getOriginalBody(definitionId: String) = repository.getOriginalBody(definitionId)
}
