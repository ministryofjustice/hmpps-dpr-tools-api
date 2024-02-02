package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource


@Configuration
@EnableTransactionManagement
class DataSourcesConfig(){

  @Bean
  @ConfigurationProperties("activities")
  fun activitiesDataSourceProperties(): DataSourceProperties {
    return DataSourceProperties()
  }

  @Bean
  @ConfigurationProperties("spring.datasource")
  fun redshiftDataSourceProperties(): DataSourceProperties {
    return DataSourceProperties()
  }

  @Bean("activities")
  fun activitiesDataSource(): DataSource? {
    return activitiesDataSourceProperties()
      .initializeDataSourceBuilder()
      .build()
  }

  @Bean(name = ["datamart", "redshift"])
  fun redshiftDataSource(): DataSource? {
    return redshiftDataSourceProperties()
      .initializeDataSourceBuilder()
      .build()
  }

  //step 4
  @Bean("activitiesTx")
  fun activitiesTransactionManager(@Qualifier("activities") dataSource: DataSource?): PlatformTransactionManager? {
    return DataSourceTransactionManager(dataSource)
  }
  @Bean("redshiftTx")
  fun redshiftTransactionManager(@Qualifier("redshift") dataSource: DataSource?): PlatformTransactionManager? {
    return DataSourceTransactionManager(dataSource)
  }

  //step 3
  @Bean("activitiesJdbc")
  fun jdbcTemplate(@Qualifier("activities") ccbsDataSource: DataSource?): JdbcTemplate? {
    return JdbcTemplate(ccbsDataSource)
  }
}
