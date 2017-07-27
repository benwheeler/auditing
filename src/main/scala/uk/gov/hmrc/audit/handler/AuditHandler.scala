package uk.gov.hmrc.audit.handler

import uk.gov.hmrc.audit.AuditResult

trait AuditHandler {
  def sendEvent(event: String): AuditResult
}
