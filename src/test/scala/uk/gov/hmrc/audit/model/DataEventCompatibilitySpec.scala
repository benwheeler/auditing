package uk.gov.hmrc.audit.model

import java.util.UUID

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import uk.gov.hmrc.http.HeaderNames

// TODO There are more tests to do, especially around the withTags and withDetail methods.
class DataEventCompatibilitySpec extends Specification {

  val uuid: String = UUID.randomUUID().toString
  val requestId: String = "request-" + UUID.randomUUID().toString
  val sessionId: String = "session-" + UUID.randomUUID().toString
  val dateTime: DateTime = DateTime.parse("2017-01-01T12:00+00:00")
  val authorisationToken = "Bearer 451b7fdf22f2e37e14c42728f1758e35"

  "When creating a DataEvent the resulting object" should {
    "Have all fields supplied in the apply function" in {
      val tags = Map[String, String](
        HeaderNames.xRequestId -> requestId,
        HeaderNames.xSessionId -> sessionId,
        "one" -> "two")
      val detail = Map[String, String]("three" -> "four")
      val auditEvent = DataEvent("source", "type", uuid, tags, detail, dateTime)

      auditEvent.auditSource mustEqual "source"
      auditEvent.auditType mustEqual "type"
      auditEvent.eventID mustEqual uuid
      auditEvent.generatedAt mustEqual dateTime
      auditEvent.requestID mustEqual requestId
      auditEvent.sessionID.get mustEqual sessionId

      auditEvent.detail mustNotEqual None
      auditEvent.detail.get.size mustEqual 2
      auditEvent.detail.get("one") mustEqual "two"
      auditEvent.detail.get("three") mustEqual "four"
    }

    "Break out the request id from the tags" in {
      val event = buildWithTags(HeaderNames.xRequestId -> requestId)
      event.detail must beNone
      event.requestID mustEqual requestId
    }

    "Break out the session id from the tags" in {
      val event = buildWithTags(HeaderNames.xSessionId -> sessionId)
      event.detail must beNone
      event.sessionID.get mustEqual sessionId
    }

    "Break out the Authorization header from the tags" in {
      val event = buildWithTags(HeaderNames.authorisation -> authorisationToken)
      event.detail must beNone
      event.authorisationToken.get mustEqual authorisationToken
    }

    "Break out a simple path from the tags" in {
      val event = buildWithTags(TagNames.path -> "/some/thing")
      event.detail must beNone
      event.path mustEqual "/some/thing"
    }

    "Break out a path with a querystring from the tags" in {
      val event = buildWithTags(TagNames.path -> "/some/thing?some=thing&other=thing")
      event.detail must beNone
      event.path mustEqual "/some/thing"
      event.queryString.get mustEqual "some=thing&other=thing"
    }

    "Hande a path with a querystring of zero length from the tags" in {
      val event = buildWithTags(TagNames.path -> "/some/thing?")
      event.detail must beNone
      event.path mustEqual "/some/thing"
      event.queryString must beNone
    }

    "Reject any event with no request id in the tags" in {
      val tags = Map[String, String]("one" -> "two")
      DataEvent("source", "type", uuid, tags, generatedAt = dateTime) must throwA[IllegalArgumentException]
    }

    "Break out a clientIP and clientPort from the tags" in {
      val event = buildWithTags(
        TagNames.clientIP -> "10.1.2.3",
        TagNames.clientPort -> "1234"
      )
      event.detail must beNone
      event.clientIP.get mustEqual "10.1.2.3"
      event.clientPort.get mustEqual 1234
    }
  }

  def buildWithTags(suppliedTags: (String, String)*): AuditEvent = {
    val tags = Map[String, String](HeaderNames.xRequestId -> requestId) ++ suppliedTags
    DataEvent("source", "type", uuid, tags, generatedAt = dateTime)
  }
}
