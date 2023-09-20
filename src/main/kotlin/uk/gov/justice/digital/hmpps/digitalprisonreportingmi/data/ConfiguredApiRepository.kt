package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
@Suppress("UNCHECKED_CAST")
class ConfiguredApiRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  fun executeQuery(
    query: String,
    rangeFilters: Map<String, String>,
    filtersExcludingRange: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String,
    sortedAsc: Boolean,
  ): List<Map<String, Any>> {
    val (preparedStatementNamedParams, whereClause) = buildWhereClause(filtersExcludingRange, rangeFilters)
    val sortingDirection = if (sortedAsc) "asc" else "desc"
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate.queryForList(
      """SELECT *
      FROM ($query) Q
      $whereClause
      ORDER BY $sortColumn $sortingDirection 
                    limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;""",
      preparedStatementNamedParams,
    )
      .map {
        transformTimestampToString(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  private fun transformTimestampToString(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime().toLocalDate().toString()
    } else {
      k to v
    }
  }

  private fun buildWhereClause(filtersExcludingRange: Map<String, String>, rangeFilters: Map<String, String>): Pair<MapSqlParameterSource, String> {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filtersExcludingRange.forEach { preparedStatementNamedParams.addValue(it.key, it.value.lowercase()) }
    rangeFilters.forEach { preparedStatementNamedParams.addValue(it.key, it.value) }
    val whereNoRange = filtersExcludingRange.keys.joinToString(" AND ") { k -> "lower($k) = :$k" }.ifEmpty { null }
    val whereRange = buildWhereRangeCondition(rangeFilters)
    //    val whereClause = whereNoRange?.let { "WHERE ${whereRange?.let { "$whereNoRange AND $whereRange"} ?: whereNoRange}" } ?: whereRange?.let { "WHERE $it" } ?: ""
    val allFilters = whereNoRange?.plus(whereRange?.let { " AND $it" } ?: "") ?: whereRange
    val whereClause = allFilters?.let { "WHERE $it" } ?: ""
    return Pair(preparedStatementNamedParams, whereClause)
  }

  private fun buildWhereRangeCondition(rangeFilters: Map<String, String>) =
    rangeFilters.keys.joinToString(" AND ") { k ->
      if (k.endsWith(".start")) {
        "${k.removeSuffix(".start")} >= :$k"
      } else if (k.endsWith(".end")) {
        "${k.removeSuffix(".end")} <= :$k"
      } else {
        throw ValidationException("Range filter does not have a .start or .end suffix: $k")
      }
    }
      .ifEmpty { null }
}