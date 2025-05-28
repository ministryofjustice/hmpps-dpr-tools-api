package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.controller

import com.google.gson.Gson
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultRequest
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultResponse
import software.amazon.awssdk.services.redshiftdata.model.StatusString
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service.DefinitionService

@RestController
@Tag(name = "Report Definition API")
class DefinitionController(
  val definitionService: DefinitionService,
  val dprDefinitionGson: Gson,
  val redshiftDataClient: RedshiftDataClient,
  val athenaClient: AthenaClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(
    description = "Saves a definition",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  @PutMapping("/definitions/{definitionId}")
  suspend fun putDefinition(
    @RequestBody
    body: String,
    @PathVariable definitionId: String,
    authentication: DprAuthAwareAuthenticationToken,
  ) {
    val definition = dprDefinitionGson.fromJson(body, ProductDefinition::class.java)

    definitionService.saveAndValidate(definition, authentication, body)
  }

  @Operation(
    description = "Deletes a definition",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  @DeleteMapping("/definitions/{definitionId}")
  fun deleteDefinition(@PathVariable definitionId: String) {
    definitionService.deleteById(definitionId)
  }

  @Operation(
    description = "Get the original definition",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  @GetMapping("/definitions/original/{definitionId}")
  fun getOriginalDefinition(@PathVariable definitionId: String) = definitionService.getOriginalBody(definitionId)

  @Operation(
    description = "Testing redshift retrieval",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  @GetMapping("/redshift/select/{column}/{executionId}")
  fun selectRedshift(@PathVariable column: String, @PathVariable executionId: String?): String {
    val whereClause = executionId?.let { "WHERE current_execution_id = '$executionId' " } ?: ""
    val result = queryRedshiftAndGetResult("SELECT * from admin.execution_manager $whereClause;")
    val columnData = getData(column, 0, result)
    log.debug("Column Data is: {}", columnData)
    log.debug("Execution ID is {}", executionId)
    return "Column Data: $columnData  \n\n execution_id: $executionId"
  }

  @GetMapping("/redshift/update/{executionId}")
  fun updateRedshift(@PathVariable executionId: String, @RequestParam state: String): String {
    log.debug("Execution ID is {}", executionId)
    val update = "UPDATE datamart.admin.execution_manager SET current_state = '$state' WHERE current_execution_id = '$executionId'"
    val status = queryRedshift(update)
    // Only result rows have a value on updates
    return "execution_id: $executionId, has_result_set: ${status.hasResulSet}, result_rows: ${status.resultRows}, result_size: ${status.resultSize}"
  }

  private fun queryRedshift(query: String): Status {
    val statementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier("dpr-redshift-development")
      .database("datamart")
      .secretArn("arn:aws:secretsmanager:eu-west-2:771283872747:secret:dpr-redshift-secret-development-rLHcQZ")
      .sql(query)
      .build()

    val executionId = redshiftDataClient.executeStatement(statementRequest).id()
    log.debug("Executed admin table statement and got ID: $executionId")
    val describeStatementRequest = DescribeStatementRequest.builder()
      .id(executionId)
      .build()
    var describeStatementResponse: DescribeStatementResponse
    do {
      Thread.sleep(500)
      describeStatementResponse = redshiftDataClient.describeStatement(describeStatementRequest)
      if (describeStatementResponse.status() == StatusString.FAILED) {
        throw RuntimeException("Statement with execution ID: $executionId failed.")
      } else if (describeStatementResponse.status() == StatusString.ABORTED) {
        throw RuntimeException("Statement with execution ID: $executionId was aborted.")
      }
    }
    while (describeStatementResponse.status() != StatusString.FINISHED)
    log.debug("Has result set: {}", describeStatementResponse.hasResultSet())
    log.debug("Total result rows: {}", describeStatementResponse.resultRows())
    log.debug("Total result size: {}", describeStatementResponse.resultSize())
    return Status(describeStatementResponse.hasResultSet(), describeStatementResponse.resultRows(), describeStatementResponse.resultSize(), executionId)
  }

  data class Status(val hasResulSet: Boolean, val resultRows: Long, val resultSize: Long, val executionId: String)

  private fun getRedshiftStatementResult(executionId: String): GetStatementResultResponse {
    val getStatementResultRequest = GetStatementResultRequest.builder()
      .id(executionId)
      .build()
    return redshiftDataClient.getStatementResult(getStatementResultRequest)
  }

  private fun queryRedshiftAndGetResult(query: String): GetStatementResultResponse = getRedshiftStatementResult(queryRedshift(query).executionId)

  private fun getData(columnName: String, rowNumber: Int, getStatementResultResponse: GetStatementResultResponse): String {
    val columnNameToResultIndex = mutableMapOf<String, Int>()
    getStatementResultResponse.columnMetadata().forEachIndexed { i, colMetaData -> columnNameToResultIndex[colMetaData.name()] = i }
    return getStatementResultResponse.records()[rowNumber][columnNameToResultIndex[columnName]!!].stringValue()
  }

  private fun queryAthena(
    query: String,
  ): String {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database("reports")
      .catalog("AwsDataCatalog")
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext(queryExecutionContext)
      .workGroup("dpr-generic-athena-workgroup")
      .build()
    log.debug("Athena query: {}", query)
    val queryExecutionId = athenaClient
      .startQueryExecution(startQueryExecutionRequest).queryExecutionId()
    return queryExecutionId
  }
}
