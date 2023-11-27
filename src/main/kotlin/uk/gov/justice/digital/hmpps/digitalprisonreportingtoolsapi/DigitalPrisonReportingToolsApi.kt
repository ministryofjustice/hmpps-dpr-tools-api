package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan("uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi", "uk.gov.justice.digital.hmpps.digitalprisonreportinglib")
class DigitalPrisonReportingToolsApi

fun main(args: Array<String>) {
  runApplication<DigitalPrisonReportingToolsApi>(*args)
}
