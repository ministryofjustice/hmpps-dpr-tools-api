package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.module.SimpleModule
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateSqlDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateSqlSerializer
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateUtilDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateUtilSerializer

@Configuration
class JacksonConfig {
  @Bean
  fun customDateModule(): SimpleModule = SimpleModule().apply {
    // java.util.Date date format
    addSerializer(java.util.Date::class.java, DateUtilSerializer())
    addDeserializer(java.util.Date::class.java, DateUtilDeserializer())

    // java.sql.Date date format
    addSerializer(java.sql.Date::class.java, DateSqlSerializer())
    addDeserializer(java.sql.Date::class.java, DateSqlDeserializer())
  }
}
