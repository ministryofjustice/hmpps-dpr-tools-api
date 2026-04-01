package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.module.SimpleModule
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.data.DateSerializer
import java.sql.Date

@Configuration
class JacksonConfiguration {

  @Bean
  fun customDateModule(): SimpleModule = SimpleModule().apply {
    addSerializer(Date::class.java, DateSerializer())
    addDeserializer(Date::class.java, DateDeserializer())
  }
}
