package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService

class FakeConfiguredApiService(
  override val productDefinitionRepository: ProductDefinitionRepository,
  override val configuredApiRepository: ConfiguredApiRepository,
) : SyncDataApiService(productDefinitionRepository, configuredApiRepository) {

  override fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: Set<String>?,
    prefix: String?,
    dataProductDefinitionsPath: String?,
    datasetForFilter: Dataset?,
  ): List<Map<String, Any?>> {
    return emptyList()
  }
}
