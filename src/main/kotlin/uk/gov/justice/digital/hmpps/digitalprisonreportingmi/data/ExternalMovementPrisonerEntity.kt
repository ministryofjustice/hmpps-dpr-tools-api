package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import java.time.LocalDateTime

data class ExternalMovementPrisonerEntity(
  val id: Long,
  val prisoner: Long,
  val firstName: String,
  val lastName: String,
  val date: LocalDateTime,
  val time: LocalDateTime,
  val origin: String?,
  val destination: String?,
  val direction: String?,
  val type: String,
  val reason: String,
)
