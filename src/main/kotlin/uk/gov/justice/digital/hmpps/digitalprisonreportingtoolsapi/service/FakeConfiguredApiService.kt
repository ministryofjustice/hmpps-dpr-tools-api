package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService

class FakeConfiguredApiService(
  override val productDefinitionRepository: ProductDefinitionRepository,
  override val configuredApiRepository: ConfiguredApiRepository
  ) : ConfiguredApiService(productDefinitionRepository, configuredApiRepository) {

  override fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userCaseloads: List<String>,
    reportFieldId: String?,
    prefix: String?
  ): List<Map<String, Any>> {
    return emptyList()
  }
}