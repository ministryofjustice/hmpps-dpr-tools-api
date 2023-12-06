package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.FilterTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.SchemaFieldTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import java.time.LocalDate

@Configuration
class GsonConfig {
  @Bean
  fun deserialiser(): Gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, IsoLocalDateTypeAdaptor())
    .registerTypeAdapter(FilterType::class.java, FilterTypeDeserializer())
    .registerTypeAdapter(ParameterType::class.java, SchemaFieldTypeDeserializer())
    .create()
}
