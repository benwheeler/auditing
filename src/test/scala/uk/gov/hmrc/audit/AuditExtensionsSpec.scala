package uk.gov.hmrc.audit

import java.util.UUID

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, RequestId, SessionId}

class AuditExtensionsSpec extends Specification with AuditExtensions {

  val eventID: String = UUID.randomUUID.toString
  val requestID: String = UUID.randomUUID.toString
  val sessionID: String = UUID.randomUUID.toString
  val auditSource: String = "source"
  val auditType: String = "type"
  val path: String = "/some/path"
  val method: String = "GET"
  val dateTime: DateTime = DateTime.parse("2017-01-01T12:00+00:00")
  val authorisationToken: String = "Bearer 451b7fdf22f2e37e14c42728f1758e35"
  val queryString: String = "foo=bar&blah=wibble"

  implicit val hc = HeaderCarrier(
    requestId = Some(RequestId(requestID)),
    sessionId = Some(SessionId(sessionID)),
    authorization = Some(Authorization(authorisationToken))
  )

  override def now: DateTime = dateTime

  override def newUUID: String = eventID

  "An audit event" should {
    "Contain the basic fields" in {
      val event = auditEvent(auditSource, auditType, path, method)
      event.auditSource mustEqual auditSource
      event.auditType mustEqual auditType
      event.path mustEqual path
      event.method mustEqual method
      event.generatedAt mustEqual dateTime
      event.eventID mustEqual eventID
    }

    "Contain the request ID from the header carrier" in {
      val event = auditEvent(auditSource, auditType, path, method)
      event.requestID mustEqual requestID
    }

    "Contain the session ID from the header carrier" in {
      val event = auditEvent(auditSource, auditType, path, method)
      event.requestID mustEqual requestID
    }

    "Split the path and querystring if they are supplied as one" in {
      val event = auditEvent(auditSource, auditType, s"$path?$queryString", method)
      event.path mustEqual path
      event.queryString must beSome(queryString)
    }

    "Not populate the querystring if the path ends in question mark" in {
      val event = auditEvent(auditSource, auditType, s"$path?", method)
      event.path mustEqual path
      event.queryString must beNone
    }

    "Contain the authorisationToken from the header carrier" in {
      val event = auditEvent(auditSource, auditType, path, method)
      event.authorisationToken must beSome(authorisationToken)
    }
  }
}
