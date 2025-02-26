package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.*
import javax.sql.DataSource

@Configuration
class RedshiftDataSourceConfiguration(
  @Value("\${customdatasource.redshift.url}") val url: String,
  @Value("\${customdatasource.redshift.username}") val username: String,
  @Value("\${customdatasource.redshift.password}") val password: String,
  @Value("\${customdatasource.redshift.driver}") val driver: String,
) {
  @Bean("redshift")
  @Primary
  fun createRedshiftDataSource(): DataSource = DataSourceBuilder.create()
    .url(url)
    .username(username)
    .password(password)
    .driverClassName(driver)
    .build()
}
