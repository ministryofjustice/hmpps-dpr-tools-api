package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.controller

enum class DPDIssueType(val description: String) {
  MISSING_FIELD("A required field is missing"),
  INVALID_FIELD_VALUE("This field has an invalid value"),
  TRAILING_COMMA("There is a trailing comma"),
  GENERAL_SYNTAX_ERROR("There is a general syntax error that cannot be otherwise detected"),
  UNKNOWN_ISSUE("There is an unknown issue"),
}

data class DPDIssue(
  val path: String,
  val issueType: DPDIssueType,
  val extraHint: String? = null,
) {
  val issueTypeDescription: String = issueType.description
}
