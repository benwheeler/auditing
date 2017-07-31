package uk.gov.hmrc.audit.model

import java.net.URI

import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderNames

/**
  * Class used for all audit events.
  *
  * This is a replacement for all the uk.gov.hmrc.play.audit.model case classes.
  */
case class AuditEvent (
  auditSource: String,
  auditType: String,

  generatedAt: DateTime,
  eventID: String,
  requestID: String,
  var sessionID: Option[String], // FIXME Rework to make val

  path: String,
  method: String,
  queryString: Option[String],
  clientIP: Option[String],
  clientPort: Option[Int],
  authorisationToken: Option[String],

  requestHeaders: Option[Map[String, String]],
  requestPayload: Option[Payload],

  identifiers: Option[Map[String, String]],
  enrolments: Option[List[Enrolment]],
  var detail: Option[Map[String, _]], // FIXME Rework to make val

  responseHeaders: Option[Map[String, String]],
  responseStatus: Option[Int],
  responsePayload: Option[Payload],
  version: Int = 1) {

  @deprecated("Use the constructor instead of this method")
  def withDetail(moreDetail: (String, String)*): AuditEvent = {
    copy(detail = Some(detail.getOrElse(Map[String, AnyVal]()) ++ moreDetail))
  }

  @deprecated("Supports legacy code which still uses the tags grouping")
  def withTags(moreTags: (String, String)*): AuditEvent = {
    moreTags.foreach[Unit](tag => {
      tag._1 match {
        case HeaderNames.xSessionId =>
          sessionID = Some(tag._2)
        case _ =>
          detail = Some(detail.getOrElse(Map[String, AnyVal]()) + tag)
      }
    })
    this
  }
}

object AuditEvent {
  def splitPath(pathString: String): (String, String) = {
    val pathUrl = new URI(pathString)
    (pathUrl.getPath, pathUrl.getQuery)
  }
}

/**
  * This is provided for information only, and is the internal format used by TxM for implicit audit events.
  */
case class ImplicitEvent(
  auditSource: String,
  auditType: String,

  generatedAt: DateTime,
  eventID: String,
  requestID: String,
  sessionID: Option[String],

  path: String,
  method: String,
  queryString: Option[String],
  clientIP: Option[String],
  clientPort: Option[Int],
  receivingIP: Option[String],
  authorisationToken: Option[String],

  clientHeaders: Option[Map[String, String]],
  requestHeaders: Option[Map[String, String]],
  cookies: Option[Map[String, String]],
  fields: Option[Map[String, String]],
  requestPayload: Option[Payload],

  identifiers: Option[Map[String, String]],
  enrolments: Option[List[Enrolment]],
  detail: Option[Map[String, _]],

  responseHeaders: Option[Map[String, String]],
  responseStatus: Option[Int],
  responsePayload: Option[Payload],
  version: Int = 1
)

case class Payload private (
  payloadType: String,
  contents: Option[String],
  reference: Option[String]
)

object Payload {
  def apply(payloadType: String, contents: String): Payload = {
    assertPayloadLength(payloadType)
    new Payload(payloadType, Some(contents), None)
  }

  def apply(payloadType: String, reference: Option[String] = None): Payload = {
    assertPayloadLength(payloadType)
    new Payload(payloadType, None, reference)
  }

  private def assertPayloadLength(payloadType: String): Unit = {
    if (payloadType.length > 100) throw new IllegalArgumentException("Payload type too long.")
  }
}

case class Enrolment(
  serviceName: String,
  identifiers: Map[String, String]
)
