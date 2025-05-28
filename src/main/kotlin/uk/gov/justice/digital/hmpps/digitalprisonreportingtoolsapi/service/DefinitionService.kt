package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ProductDefinitionTokenPolicyChecker
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.alert.AlertCategoryCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.CrudProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.InvalidDefinitionException

@Service
class DefinitionService(
  private val repository: CrudProductDefinitionRepository,
  dataRepository: ConfiguredApiRepository,
  identifiedHelper: IdentifiedHelper,
  establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
  productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
  alertCategoryCacheService: AlertCategoryCacheService,
) {
  val mapper: ReportDefinitionMapper = ReportDefinitionMapper(FakeConfiguredApiService(repository, dataRepository, productDefinitionTokenPolicyChecker, identifiedHelper), identifiedHelper, establishmentCodesToWingsCacheService, alertCategoryCacheService)

  suspend fun saveAndValidate(
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
        .map { mapper.mapReport(it, userToken = authenticationToken) }
    } catch (e: Exception) {
      try {
        definition.id.let {
          repository.deleteById(definition.id)
        }
      } finally {
        throw InvalidDefinitionException(e)
      }
    }
  }

  fun deleteById(definitionId: String) {
    repository.deleteById(definitionId)
  }

  fun getOriginalBody(definitionId: String) = repository.getOriginalBody(definitionId)
}
