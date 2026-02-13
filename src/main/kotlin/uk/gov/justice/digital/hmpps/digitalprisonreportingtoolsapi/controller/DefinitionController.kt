package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.controller

import com.google.gson.Gson
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.service.DefinitionService

@RestController
@Tag(name = "Report Definition API")
class DefinitionController(
  val definitionService: DefinitionService,
  val dprDefinitionGson: Gson,
) {
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
    println("Controller token class: ${authentication.javaClass}")
    println("Controller token class name: ${authentication.javaClass.name}")
    val principal = SecurityContextHolder.getContext().authentication
    println("SecurityContext authentication class: ${principal.javaClass.name}")
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
}
