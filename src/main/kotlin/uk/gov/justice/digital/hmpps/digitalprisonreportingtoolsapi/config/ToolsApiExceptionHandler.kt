package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.DefinitionNotFoundException
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception.InvalidDefinitionException

@RestControllerAdvice
class ToolsApiExceptionHandler {
  @ExceptionHandler(MismatchedInputException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> = respondWithBadRequest(e)

  @ExceptionHandler(InvalidDefinitionException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handleInvalidDefinitionException(e: Exception): ResponseEntity<ErrorResponse> = respondWithBadRequest(e)

  @ExceptionHandler(DefinitionNotFoundException::class)
  @ResponseStatus(NOT_FOUND)
  fun handleDefinitionNotFoundException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = e.message,
        developerMessage = e.message,
      ),
    )

  private fun respondWithBadRequest(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    log.info("Cause: {}", e.cause?.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
