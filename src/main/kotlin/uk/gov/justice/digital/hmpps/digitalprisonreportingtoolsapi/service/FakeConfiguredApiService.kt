package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ProductDefinitionTokenPolicyChecker
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService

class FakeConfiguredApiService(
  productDefinitionRepository: ProductDefinitionRepository,
  configuredApiRepository: ConfiguredApiRepository,
  productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
) : SyncDataApiService(productDefinitionRepository, configuredApiRepository, productDefinitionTokenPolicyChecker) {

  override suspend fun validateAndFetchData(
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
