package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data

import com.google.gson.Gson
import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AbstractProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config.RedshiftDataSourceConfiguration
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.DefinitionNotFoundException
import javax.sql.DataSource

@ConditionalOnProperty("dpr.lib.definition.redshift.enabled", havingValue = "true")
@ConditionalOnBean(RedshiftDataSourceConfiguration::class)
@Service
class RedshiftProductDefinitionRepository(
  identifiedHelper: IdentifiedHelper,
  val dprDefinitionGson: Gson,
  @Value("\${dpr.lib.definition.redshift.database:datamart}") private val database: String,
  @Value("\${dpr.lib.definition.redshift.schema:product}") private val schema: String,
  @Value("\${dpr.lib.definition.redshift.table:data_product }") private val table: String,
) : AbstractProductDefinitionRepository(identifiedHelper),
  CrudProductDefinitionRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  @Qualifier("redshift")
  lateinit var dataSource: DataSource

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    log.debug("Fetching definitions from Redshift.")
    val stopwatch = StopWatch.createStarted()
    val jdbcTemplate = JdbcTemplate(dataSource)
    val definitions: List<ProductDefinition> = jdbcTemplate.queryForList(
      "SELECT DEFINITION FROM $database.$schema.$table;",
    ).map {
      it.entries.associate { (k, v) -> k to dprDefinitionGson.fromJson(v as String, ProductDefinition::class.java) }
    }.flatMap { it.values }
    stopwatch.stop()
    log.debug("Retrieved definitions from Redshift in {} ms.", stopwatch.time)
    return definitions
  }

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition {
    try {
      val stopwatch = StopWatch.createStarted()
      val jdbcTemplate = JdbcTemplate(dataSource)
      val sql = "SELECT DEFINITION FROM $database.$schema.$table WHERE ID=?"
      log.debug("Retrieving definition with id $definitionId from Redshift.")
      log.debug("SQL query: $sql")
      val definition = jdbcTemplate.queryForObject(
        sql,
        { rs, _ ->
          dprDefinitionGson.fromJson(rs.getString("definition"), ProductDefinition::class.java)
        },
        definitionId,
      )
      log.debug("Retrieved definition with id: $definitionId from Redshift in {} ms.", stopwatch.time)
      return definition
    } catch (e: EmptyResultDataAccessException) {
      throw DefinitionNotFoundException("Invalid report id provided: $definitionId")
    }
  }

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String, dataProductDefinitionsPath: String?): SingleReportProductDefinition {
    try {
      return super.getSingleReportProductDefinition(definitionId, reportId, null)
    } catch (e: ValidationException) {
      throw DefinitionNotFoundException(e.message)
    }
  }

  override fun save(definition: ProductDefinition, originalBody: String) {
    val jdbcTemplate = JdbcTemplate(dataSource)
    log.debug("Saving definition with id ${definition.id} into Redshift.")
    val stopwatch = StopWatch.createStarted()
    val sql = """
        MERGE INTO $database.$schema.$table
        USING (SELECT cast(? as VARCHAR(max)) as ID, json_parse(?) as DEFINITION) as source
        on $database.$schema.$table.ID = source.ID 
        WHEN MATCHED THEN
        UPDATE SET ID = source.ID, DEFINITION = source.DEFINITION
        WHEN NOT MATCHED 
        THEN INSERT VALUES (source.ID, source.DEFINITION)
    """.trimIndent()
    log.debug("SQL query: $sql")
    jdbcTemplate.update(sql, definition.id, originalBody)
    stopwatch.stop()
    log.debug("Saved definition into Redshift in {} ms.", stopwatch.time)
  }

  @Order
  override fun deleteById(definitionId: String) {
    log.debug("Deleting definition with id $definitionId from Redshift.")
    val stopwatch = StopWatch.createStarted()
    val jdbcTemplate = JdbcTemplate(dataSource)
    val sql = "DELETE FROM $database.$schema.$table WHERE ID=?"
    log.debug("SQL query: $sql")
    val deletedRows = jdbcTemplate.update(
      sql,
      definitionId,
    )
    stopwatch.stop()
    log.debug("Deleted $deletedRows rows with definition with ID: $definitionId from Redshift in {}.", stopwatch.time)
  }

  override fun getOriginalBody(definitionId: String): String? {
    try {
      val stopwatch = StopWatch.createStarted()
      val jdbcTemplate = JdbcTemplate(dataSource)
      val sql = "SELECT DEFINITION FROM $database.$schema.$table WHERE ID=?"
      log.debug("Retrieving original_body with id $definitionId from Redshift.")
      log.debug("SQL query: $sql")
      val originalBody = jdbcTemplate.queryForObject(
        sql,
        { rs, _ ->
          rs.getString("definition")
        },
        definitionId,
      )
      log.debug("Retrieved original body with definition id: $definitionId from Redshift in {} ms.", stopwatch.time)
      return originalBody
    } catch (e: EmptyResultDataAccessException) {
      throw DefinitionNotFoundException("Invalid report id provided: $definitionId")
    }
  }
}
