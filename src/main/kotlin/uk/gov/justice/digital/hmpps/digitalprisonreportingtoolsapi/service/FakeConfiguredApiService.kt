package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.*
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

class FakeConfiguredApiService(
  override val productDefinitionRepository: ProductDefinitionRepository,
  override val configuredApiRepository: ConfiguredApiRepository,
  override val athenaApiRepository: AthenaApiRepository,
  override val redshiftDataApiRepository: RedshiftDataApiRepository,
  override val tableIdGenerator: TableIdGenerator,
  override val datasetHelper: DatasetHelper,
) : ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper) {

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
