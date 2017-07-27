package uk.gov.hmrc.audit

sealed trait AuditResult
object AuditResult {
  case object Success extends AuditResult
  case object Rejected extends AuditResult
  case object Failure extends AuditResult
}
