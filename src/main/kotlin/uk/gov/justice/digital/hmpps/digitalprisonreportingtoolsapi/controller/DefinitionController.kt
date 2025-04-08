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
  @GetMapping("/redshift/test/{column}")
  fun testRedshift(@PathVariable column: String, @RequestParam executionId: String?): String {
    val whereClause = executionId?.let { "WHERE current_execution_id = $executionId " } ?: ""
    val result = queryRedshiftAndGetResult("SELECT * from admin.execution_manager $whereClause;")
    return getData("column", 0, result)
  }

  private fun queryRedshift(query: String): String {
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
    return executionId
  }

  private fun getRedshiftStatementResult(executionId: String): GetStatementResultResponse {
    val getStatementResultRequest = GetStatementResultRequest.builder()
      .id(executionId)
      .build()
    return redshiftDataClient.getStatementResult(getStatementResultRequest)
  }

  private fun queryRedshiftAndGetResult(query: String): GetStatementResultResponse = getRedshiftStatementResult(queryRedshift(query))

  private fun getData(columnName: String, rowNumber: Int, getStatementResultResponse: GetStatementResultResponse): String {
    val columnNameToResultIndex = mutableMapOf<String, Int>()
    getStatementResultResponse.columnMetadata().forEachIndexed { i, colMetaData -> columnNameToResultIndex[colMetaData.name()] = i }
    return getStatementResultResponse.records()[rowNumber][columnNameToResultIndex[columnName]!!].stringValue()
  }
}
