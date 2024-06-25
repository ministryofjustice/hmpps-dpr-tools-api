package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService

class FakeConfiguredApiService(
  override val productDefinitionRepository: ProductDefinitionRepository,
  override val configuredApiRepository: ConfiguredApiRepository,
  override val athenaApiRepository: AthenaApiRepository,
  override val redshiftDataApiRepository: RedshiftDataApiRepository,
) : ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository) {

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
