package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import javax.sql.DataSource

@Configuration
class ActivitiesDataSourceConfiguration(
  @Value("\${customdatasource.activities.url}") val url: String,
  @Value("\${customdatasource.activities.username}") val username: String,
  @Value("\${customdatasource.activities.password}") val password: String,
  @Value("\${customdatasource.activities.driver}") val driver: String,
) {
  @Bean("activities")
  @ConditionalOnProperty("customdatasource.activities.enabled", havingValue = "true")
  fun createCustomDataSource(): DataSource {
    return DataSourceBuilder.create()
      .url(url)
      .username(username)
      .password(password)
      .driverClassName(driver)
      .build()
  }
}
