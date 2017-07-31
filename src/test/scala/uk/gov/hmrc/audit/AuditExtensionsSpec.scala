package uk.gov.hmrc.audit

import java.util.UUID

import org.joda.time.DateTime
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import uk.gov.hmrc.audit.model.{Enrolment, Payload}
import uk.gov.hmrc.http._

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
      val event = auditEvent(auditSource, auditType, method, path)
      event.auditSource mustEqual auditSource
      event.auditType mustEqual auditType
      event.path mustEqual path
      event.method mustEqual method
      event.generatedAt mustEqual dateTime
      event.eventID mustEqual eventID
    }

    "Contain the request ID from the header carrier" in {
      val event = auditEvent(auditSource, auditType, method, path)
      event.requestID mustEqual requestID
    }

    "Contain the session ID from the header carrier" in {
      val event = auditEvent(auditSource, auditType, method, path)
      event.requestID mustEqual requestID
    }

    "Split the path and querystring if they are supplied as one" in {
      val event = auditEvent(auditSource, auditType, method, s"$path?$queryString")
      event.path mustEqual path
      event.queryString must beSome(queryString)
    }

    "Not populate the querystring if the path ends in question mark" in {
      val event = auditEvent(auditSource, auditType, method, s"$path?")
      event.path mustEqual path
      event.queryString must beNone
    }

    "Contain the authorisationToken from the header carrier" in {
      val event = auditEvent(auditSource, auditType, method, path)
      event.authorisationToken must beSome(authorisationToken)
    }

    "Contain request headers from the header carrier, and from explicit request headers" in {
      val carrier = HeaderCarrier(
        requestId = Some(RequestId(requestID)),
        forwarded = Some(ForwardedFor("forwardedValue")),
        akamaiReputation = Some(AkamaiReputation("akamaiValue")),
        otherHeaders = Seq("header1" -> "value1")
      )

      val event = auditEvent(auditSource, auditType, method, path, requestHeaders = Some(Map(
        "header2" -> "value2"
      )))(carrier)
      event.requestHeaders.get("header1") mustEqual "value1"
      event.requestHeaders.get("header2") mustEqual "value2"
      event.requestHeaders.get(HeaderNames.akamaiReputation) mustEqual "akamaiValue"
      event.requestHeaders.get(HeaderNames.xForwardedFor) mustEqual "forwardedValue"
      event.requestHeaders.get.size mustEqual 4
    }

    "Extract IP and port from header carrier" in {
      val carrier = HeaderCarrier(
        requestId = Some(RequestId(requestID)),
        trueClientIp = Some("10.1.2.3"),
        trueClientPort = Some("12345")
      )

      val event = auditEvent(auditSource, auditType, method, path)(carrier)
      event.clientIP.get mustEqual "10.1.2.3"
      event.clientPort.get mustEqual 12345
      event.requestHeaders must beNone
    }

    "Ignores empty request headers from the header carrier" in {
      val carrier = HeaderCarrier(
        requestId = Some(RequestId(requestID)),
        forwarded = Some(ForwardedFor("")),
        akamaiReputation = Some(AkamaiReputation("")),
        otherHeaders = Seq(
          "header1" -> "",
          "header2" -> "-",
          "" -> "emptyKey1")
      )

      val event = auditEvent(auditSource, auditType, method, path, requestHeaders = Some(Map(
        "header3" -> "",
        "header4" -> "-",
        "" -> "emptyKey2"
      )))(carrier)
      event.requestHeaders must beNone
    }

    "Contain payloads if they were supplied" in {
      val requestPayload = Some(Payload("application/json", "{\"ohai\": \"gimmeh\"}"))
      val responsePayload = Some(Payload("application/json", "{\"icanhaz\": \"kthxbye\"}"))
      val event = auditEvent(auditSource, auditType, method, path,
        requestPayload = requestPayload,
        responsePayload = responsePayload
      )
      event.requestPayload mustEqual requestPayload
      event.responsePayload mustEqual responsePayload
    }

    "Contain identifiers if they were supplied" in {
      val identifiers = Some(Map("id1" -> "value1"))
      val event = auditEvent(auditSource, auditType, method, path, identifiers = identifiers)
      event.identifiers.get("id1") mustEqual "value1"
      event.identifiers.get.size mustEqual 1
    }

    "Not contain empty identifiers if they were supplied" in {
      val identifiers = Some(Map(
        "" -> "value1",
        "id1" -> "",
        "id2" -> "-"))
      val event = auditEvent(auditSource, auditType, method, path, identifiers = identifiers)
      event.identifiers must beNone
    }

    "Contain enrolments if they were supplied" in {
      val enrolments = Some(List(Enrolment("IR-SERVICE", Map("id1" -> "value1"))))
      val event = auditEvent(auditSource, auditType, method, path, enrolments = enrolments)
      event.enrolments mustEqual enrolments
    }

    "Contain detail fields if they were supplied" in {
      val suppliedDetail = Map("detail1" -> "value1")
      val event = auditEvent(auditSource, auditType, method, path, detail = suppliedDetail)
      event.detail.get mustEqual suppliedDetail
    }

    "Allow nested detail fields" in {
      val nestedDetail = Map("detailGroup1" -> Map("detailKey1" -> "value1"))
      val event = auditEvent(auditSource, auditType, method, path, detail = nestedDetail)
      event.detail.get mustEqual nestedDetail
    }

    "Not contain a detail value if there were no fields populated" in {
      val suppliedDetail = Map(
        "" -> "value1",
        "supplied1" -> "",
        "supplied2" -> "-",
        "nested1" -> Map())
      val event = auditEvent(auditSource, auditType, method, path, detail = suppliedDetail)
      event.detail must beNone
    }

    "Redirect identifiers from the detail section if they were supplied" in {
      Result.foreach(AuditExtensions.detailsToIdentifiers.keys.toSeq) { key => {
        val mappedIdentifierKey = AuditExtensions.detailsToIdentifiers(key)
        val detailWithId = Map(key -> s"${key}value1")
        val event = auditEvent(auditSource, auditType, method, path, detail = detailWithId)
        event.detail must beEmpty
        event.identifiers.get(mappedIdentifierKey) mustEqual s"${key}value1"
      }}
    }

    "Contain responseHeaders if they were supplied" in {
      val responseHeaders = Some(Map("header1" -> "value1"))
      val event = auditEvent(auditSource, auditType, method, path, responseHeaders = responseHeaders)
      event.responseHeaders mustEqual responseHeaders
    }

    "Contain responseStatus if it was supplied" in {
      val event = auditEvent(auditSource, auditType, method, path, responseStatus = Some(666))
      event.responseStatus.get mustEqual 666
    }
  }
}
