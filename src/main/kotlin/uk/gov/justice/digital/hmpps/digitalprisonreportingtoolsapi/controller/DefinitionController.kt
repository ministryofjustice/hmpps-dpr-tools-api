package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service.DefinitionService

@RestController
@Tag(name = "Report Definition API")
class DefinitionController(val definitionService: DefinitionService) {
  @Operation(
    description = "Saves a definition",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  @PutMapping("/definition")
  fun putDefinition(
    @RequestBody
    @Valid
    definition: ProductDefinition,
  ) {
    definitionService.validateAndSave(definition)
  }

  @Operation(
    description = "Deletes a definition",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  @DeleteMapping("/definition/{definitionId}")
  fun deleteDefinition(@PathVariable definitionId: String) {
    definitionService.deleteById(definitionId)
  }
}
