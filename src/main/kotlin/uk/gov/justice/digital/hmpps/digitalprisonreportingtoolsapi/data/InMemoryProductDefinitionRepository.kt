package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import jakarta.validation.ValidationException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AbstractProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportLite
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.DefinitionNotFoundException
import java.util.concurrent.ConcurrentHashMap

@ConditionalOnProperty("dpr.lib.definition.repository", havingValue = "memory")
@Service
@Primary
class InMemoryProductDefinitionRepository(identifiedHelper: IdentifiedHelper) :
  AbstractProductDefinitionRepository(identifiedHelper),
  CrudProductDefinitionRepository {

  private val definitions: ConcurrentHashMap<String, Pair<ProductDefinition, String>> = ConcurrentHashMap()

  override fun getProductDefinitions(path: String?): List<ProductDefinitionSummary> = definitions.values.map { it.first }.map { it.mapToSummary() }.toList()

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition = definitions.getOrElse(definitionId) { throw DefinitionNotFoundException("Invalid report id provided: $definitionId") }.first

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String?): SingleReportProductDefinition {
    try {
      return super.getSingleReportProductDefinition(definitionId, reportId, null)
    } catch (e: ValidationException) {
      throw DefinitionNotFoundException(e.message)
    }
  }

  override fun save(definition: ProductDefinition, originalBody: String) {
    definitions[definition.id] = Pair(definition, originalBody)
  }

  override fun deleteById(definitionId: String) {
    definitions.remove(definitionId)
  }

  override fun getOriginalBody(definitionId: String) = definitions[definitionId]?.second

  fun ProductDefinition.mapToSummary() = ProductDefinitionSummary(
    id = this.id,
    name = this.name,
    description = this.description,
    metadata = this.metadata,
    path = this.path,
    dataset = this.dataset,
    report = mapReports(this.report),
    policy = this.policy,
    dashboard = this.dashboard,
  )

  private fun mapReports(reports: List<Report>) = reports.map { report ->
    ReportLite(
      id = report.id,
      name = report.name,
      description = report.description,
      dataset = report.dataset,
      render = report.render,
    )
  }.toList()
}
