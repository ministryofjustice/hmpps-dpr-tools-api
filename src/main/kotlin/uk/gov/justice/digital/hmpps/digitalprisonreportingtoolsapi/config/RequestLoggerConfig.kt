package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RequestLoggerConfig : Filter {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val httpRequest = request as HttpServletRequest
    val requestURI = httpRequest.requestURI
    val isNotHealthRequest = !requestURI.startsWith("/health")
    if (isNotHealthRequest) {
      log.debug("Http Request: ${httpRequest.method} $requestURI")
    }
    chain.doFilter(request, response)
  }
}
