package uk.gov.hmrc.audit

import uk.gov.hmrc.audit.connector.AuditConnector
import uk.gov.hmrc.audit.model.{AuditEvent, Enrolment, Payload}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

/**
  * The intent of these extensions are to provide quite a lot of shortcuts for
  * services to use when they create audit events. It should not be necessary for
  * them to wrap the HeaderCarrier, or create any subordinate things like DataCall
  * objects.
  *
  * It should also not be possible to create a malformed audit event, or one that
  * does not contain any required fields.
  */
trait AuditExtensions {

  def send(event: AuditEvent)(implicit auditConnector: AuditConnector, ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendEvent(event)
  }

  def auditEvent(auditSource: String, auditType: String, method: String,
    pathString: String, detail: Map[String, _] = Map(), identifiers: Option[Map[String, String]] = None,
    enrolments: Option[List[Enrolment]] = None, requestHeaders: Option[Map[String, String]] = None,
    requestPayload: Option[Payload] = None, responseHeaders: Option[Map[String, String]] = None,
    responsePayload: Option[Payload] = None, responseStatus: Option[Int] = None,
    clientIP: Option[String] = None, clientPort: Option[Int] = None
  )(implicit hc: HeaderCarrier): AuditEvent = AuditEvent.apply(auditSource, auditType, method,
    pathString, detail = detail, identifiers = identifiers, enrolments = enrolments,
    requestHeaders = requestHeaders, requestPayload = requestPayload, responseHeaders = responseHeaders,
    responsePayload = responsePayload, responseStatus = responseStatus, clientIP = clientIP,
    clientPort = clientPort
  )
}

object AuditExtensions {
  val excludedRequestHeaders: Seq[String] = Seq(
    // add any header exclusions here
  )

  val detailsToIdentifiers: Map[String, String] = Map(
    "credId" -> "credID"
  )
}
