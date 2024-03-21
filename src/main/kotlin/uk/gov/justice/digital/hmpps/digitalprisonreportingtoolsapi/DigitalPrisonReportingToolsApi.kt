package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DigitalPrisonReportingToolsApi

fun main(args: Array<String>) {
  runApplication<DigitalPrisonReportingToolsApi>(*args)
}
