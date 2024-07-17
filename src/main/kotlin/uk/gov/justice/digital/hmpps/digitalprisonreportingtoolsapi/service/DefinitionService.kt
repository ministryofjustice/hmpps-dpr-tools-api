package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DatasetHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.InMemoryProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.InvalidDefinitionException

@Service
class DefinitionService(
  private val repository: InMemoryProductDefinitionRepository,
  private val dataRepository: ConfiguredApiRepository,
  athenaApiRepository: AthenaApiRepository,
  redshiftDataApiRepository: RedshiftDataApiRepository,
  tableIdGenerator: TableIdGenerator,
  datasetHelper: DatasetHelper,
) {
  val mapper: ReportDefinitionMapper = ReportDefinitionMapper(FakeConfiguredApiService(repository, dataRepository, athenaApiRepository, redshiftDataApiRepository, tableIdGenerator, datasetHelper), datasetHelper)

  fun saveAndValidate(
    definition: ProductDefinition,
    authenticationToken: DprAuthAwareAuthenticationToken,
    originalBody: String,
  ) {
    try {
      definition.id.let {
        repository.save(definition, originalBody)
      }
      definition.report
        .map { report -> repository.getSingleReportProductDefinition(definitionId = definition.id, report.id) }
        // Attempt mapping to assert references are correct
        .map { mapper.map(it, userToken = authenticationToken) }

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
      definition.id.let {
        repository.deleteById(definition.id)
      }
      throw InvalidDefinitionException(e)
    }
  }

  fun deleteById(definitionId: String) {
    repository.deleteById(definitionId)
  }

  fun getOriginalBody(definitionId: String) = repository.getOriginalBody(definitionId)
}
