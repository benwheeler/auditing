package uk.gov.hmrc.audit.model

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.time.DateTimeUtils

object LegacyTagNames {
  val path = "path"
}

object LegacyDetailNames {
  val method = "method"
  val statusCode = "method"
  val requestBody = "requestBody"
  val responseMessage = "responseMessage"
  val akamaiReputation = "Akamai-Reputation"
}

/**
  * Provides backward compatibility with the old DataEvent case class. Designed
  * to be a drop in replacement for uk.gov.hmrc.play.audit.model.DataEvent#apply().
  */
object DataEvent {
  def apply(auditSource: String,
    auditType: String,
    eventId: String = UUID.randomUUID().toString,
    tags: Map[String, String] = Map.empty,
    detail: Map[String, String] = Map.empty,
    generatedAt: DateTime = DateTimeUtils.now): AuditEvent = {

    val sessionId = if (tags.contains(HeaderNames.xSessionId)) Some(tags(HeaderNames.xSessionId)) else None
    val authorisationToken = if (tags.contains(HeaderNames.authorisation)) Some(tags(HeaderNames.authorisation)) else None

    val pathTag = tags.getOrElse("path", "")

    val questionIndex = pathTag.indexOf("?")
    val queryString = if (questionIndex == -1 || questionIndex == pathTag.length - 1) None else Some(pathTag.substring(questionIndex + 1))
    val path = if (questionIndex == -1) pathTag else pathTag.substring(0, questionIndex)

    val requestId = tags.getOrElse(HeaderNames.xRequestId, "")
    if (requestId == "") {
      // TODO Can we treat this as an error?
      throw new IllegalArgumentException(s"Tags must contain a valid ${HeaderNames.xRequestId}.")
    }

    AuditEvent(auditSource,
      auditType,
      generatedAt,
      eventId,
      requestId,
      sessionId,
      path,
      "method",
      queryString,
      authorisationToken,
      None,
      None,
      None,
      None,
      Some(detail),
      None,
      Some(1), // responseStatus
      None
    ).withTags(tags.filterNot(tag => tag._1 match {
      // FIXME There must be a better was to do this... see below in the withTags method for the same concept.
      case HeaderNames.xSessionId => true
      case HeaderNames.authorisation => true
      case LegacyTagNames.path => true
      case HeaderNames.xRequestId => true
      case _ => false
    }).toArray:_*)
  }
}

/**
  * Class used for all audit events.
  *
  * This is a replacement for uk.gov.hmrc.play.audit.model.AuditEvent.
  */
case class AuditEvent(
  auditSource: String,
  auditType: String,

  generatedAt: DateTime,
  eventID: String,
  requestID: String,
  var sessionID: Option[String], // FIXME Rework to make val

  path: String,
  method: String,
  queryString: Option[String],
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

  def withDetail(moreDetail: (String, String)*): AuditEvent = copy(detail = Some(detail.getOrElse(Map[String, AnyVal]()) ++ moreDetail))

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
  clientIP: String,
  clientPort: Int,
  receivingIP: String,
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

case class Payload(
  `type`: String,
  contents: Option[String],
  reference: Option[String]
)

case class Enrolment(
  name: String,
  value: String
)
