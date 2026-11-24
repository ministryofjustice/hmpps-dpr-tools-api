package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.getExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.DataProductReportableInformation
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.serialization.SerialisationService
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service.DefinitionService

@RestController
@Tag(name = "Report Definition API")
class DefinitionController(
  val definitionService: DefinitionService,
  val manageUsersClient: ManageUsersClient,
  val serialisationService: SerialisationService,
  @Value($$"${dpr.lib.hasProbationDatasources}")
  val hasProbationDatasources: Boolean,
) {
  @Operation(
    description = "Saves a definition",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  @PutMapping("/definitions")
  suspend fun putDefinition(
    @RequestBody
    body: String,
    httpRequest: HttpServletRequest,
  ): ResponseEntity<List<DPDIssue>> {
    // Do proper validation, which will throw if wrong. Handled in the exception handler
    val (definition, issues) = serialisationService.deserializeProductDefinition(body, mutableListOf())
    if (definition != null) {
      definitionService.saveAndValidate(
        definition,
        httpRequest.getExecutionContext(
          manageUsersClient,
          hasProbationDatasources,
          DataProductReportableInformation(
            id = "",
            variantId = "",
          ),
        ),
        body,
      )
    }
    val status = if (definition != null) 200 else 400
    return ResponseEntity.status(status).body(issues)
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
}
