package uk.gov.hmrc.audit.model

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.audit.model.DataCall
import uk.gov.hmrc.time.DateTimeUtils

/**
 * Tag names that will be excluded from being copied into the details grouping.
 */
object LegacyTagNames {
  val path = "path"
  val clientIP = "clientIP"
  val clientPort = "clientPort"
  val transactionName = "transactionName"
  val requestID = "X-Request-ID"
  val sessionID = "X-Session-ID"

  val excludedFromDetail = Seq(path, clientIP, clientPort, transactionName, requestID, sessionID)
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
@deprecated("Use class uk.gov.hmrc.audit.model.AuditEvent")
object MergedDataEvent {
  def apply(auditSource: String,
    auditType: String,
    eventId: String = UUID.randomUUID().toString,
    request: DataCall,
    response: DataCall): AuditEvent = {

    val requestId = ""
    val sessionId = ""
    val path = ""
    val method = ""
    val queryString = ""
    val authorisationToken = ""
    val detail = Map[String, String]()
    AuditEvent(auditSource,
      auditType,
      DateTime.now(),
      eventId,
      requestId,
      Some(sessionId),
      path,
      method,
      Some(queryString),
      None,
      None,
      Some(authorisationToken),
      None,
      None,
      None,
      None,
      Some(detail),
      None,
      Some(1), // responseStatus
      None
    )
  }
}

/**
  * Provides backward compatibility with the old DataEvent case class. Designed
  * to be a drop in replacement for uk.gov.hmrc.play.audit.model.DataEvent#apply().
  */
@deprecated("Use class uk.gov.hmrc.audit.model.AuditEvent")
object DataEvent {
  def apply(auditSource: String,
    auditType: String,
    eventId: String = UUID.randomUUID().toString,
    tags: Map[String, String] = Map.empty,
    detail: Map[String, String] = Map.empty,
    generatedAt: DateTime = DateTimeUtils.now): AuditEvent = {

    val sessionId = if (tags.contains(HeaderNames.xSessionId)) Some(tags(HeaderNames.xSessionId)) else None
    val authorisationToken = if (tags.contains(HeaderNames.authorisation)) Some(tags(HeaderNames.authorisation)) else None
    val requestId = tags.getOrElse(HeaderNames.xRequestId, "")

    AuditEvent(auditSource,
      auditType,
      generatedAt,
      eventId,
      requestId,
      sessionId,
      tags.getOrElse("path", ""),
      "method",
      None,
      None,
      None,
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
