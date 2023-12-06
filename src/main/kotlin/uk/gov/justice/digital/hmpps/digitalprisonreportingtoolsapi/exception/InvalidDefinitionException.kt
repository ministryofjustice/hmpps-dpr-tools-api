package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.exception

import java.lang.Exception

class InvalidDefinitionException(innerException: Exception) : RuntimeException(innerException)
