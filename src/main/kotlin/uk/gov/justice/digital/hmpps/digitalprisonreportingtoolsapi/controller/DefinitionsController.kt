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
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.InMemoryProductDefinitionRepository

@RestController
@Tag(name = "Report Definition API")
class DefinitionsController(val repository: InMemoryProductDefinitionRepository) {
  @Operation(
    description = "Saves a definition",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  @PutMapping("/definitions")
  fun putDefinition(
    @RequestBody
    @Valid
    definition: ProductDefinition,
  ) {
    repository.save(definition)
  }

  @Operation(
    description = "Deletes a definition",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  @DeleteMapping("/definitions/{definitionId}")
  fun deleteDefinition(@PathVariable definitionId: String) {
    repository.deleteById(definitionId)
  }
}
