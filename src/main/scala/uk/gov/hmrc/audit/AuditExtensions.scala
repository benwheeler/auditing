package uk.gov.hmrc.audit

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.audit.model.AuditEvent
import uk.gov.hmrc.http.HeaderCarrier

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

  def auditEvent(auditSource: String, auditType: String, path: String,
    method: String, queryString: Option[String] = None,
    detail: Map[String, AnyVal] = Map()
  )(implicit hc: HeaderCarrier): AuditEvent = {

    if (hc.requestId.isEmpty) throw new IllegalArgumentException("Request ID must be defined.")

    val requestHeaders = None
    val identifiers = None
    val enrolments = None
    val filteredDetail = None
    val responseHeaders = None
    val responseStatus = None

    AuditEvent(auditSource,
      auditType,
      now,
      newUUID,
      hc.requestId.get.value,
      hc.sessionId.flatMap(sessionId => Some(sessionId.value)),
      path,
      method,
      queryString,
      hc.authorization.flatMap(authorisation => Some(authorisation.value)),
      requestHeaders,
      None, // no request payload
      identifiers,
      enrolments,
      filteredDetail,
      responseHeaders,
      responseStatus,
      None // no response payload
    )
    /*
    authorization: Option[Authorization] = None,
                         userId: Option[UserId] = None,
                         token: Option[Token] = None,
                         forwarded: Option[ForwardedFor] = None,
                         sessionId: Option[SessionId] = None,
                         requestId: Option[RequestId] = None,
                         requestChain: RequestChain = RequestChain.init,
                         nsStamp: Long = System.nanoTime(),
                         extraHeaders: Seq[(String, String)] = Seq(),
                         trueClientIp: Option[String] = None,
                         trueClientPort: Option[String] = None,
                         gaToken: Option[String] = None,
                         gaUserId: Option[String] = None,
                         deviceID: Option[String] = None,
                         akamaiReputation: Option[AkamaiReputation] = None,
                         otherHeaders: Seq[(String, String)] = Seq()
     */
  }

  def now: DateTime = {
    DateTime.now()
  }

  def newUUID: String = {
    UUID.randomUUID.toString
  }
}
