package uk.gov.hmrc.audit.model

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.model.DataCall
import uk.gov.hmrc.time.DateTimeUtils

object TagNames {
  val path = "path"
  val clientIP = "clientIP"
  val clientPort = "clientPort"
  val transactionName = "transactionName"
  val requestID: String = HeaderNames.xRequestId
  val sessionID: String = HeaderNames.xSessionId
  val authorisation: String = HeaderNames.authorisation

  val excludedFromDetail = Seq(path, clientIP, clientPort, transactionName, requestID, sessionID, authorisation)
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
    response: DataCall)(implicit hc: HeaderCarrier = HeaderCarrier()): AuditEvent = {

    val requestId = ""
    val sessionId = ""
    val path = ""
    val method = ""
    val queryString = ""
    val authorisationToken = ""
    val detail = Map[String, String]()
    AuditEvent(auditSource,
      auditType,
      method,
      path,
      detail = detail,
      eventID = eventId
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
    generatedAt: DateTime = DateTimeUtils.now)(implicit hc: HeaderCarrier = HeaderCarrier()): AuditEvent = {

    val sessionId = if (tags.contains(TagNames.sessionID)) Some(tags(TagNames.sessionID)) else None
    val authorisationToken = if (tags.contains(TagNames.authorisation)) Some(tags(TagNames.authorisation)) else None
    val requestId = Some(tags.getOrElse(TagNames.requestID, ""))
    val clientIP = if (tags.contains(TagNames.clientIP)) Some(tags(TagNames.clientIP)) else None
    val clientPort = if (tags.contains(TagNames.clientPort)) Some(tags(TagNames.clientPort).toInt) else None

    AuditEvent(auditSource,
      auditType,
      "method",
      tags.getOrElse("path", ""),
      generatedAt,
      detail,
      eventID = eventId,
      requestID = requestId,
      sessionID = sessionId,
      authorisationToken = authorisationToken,
      clientIP = clientIP,
      clientPort = clientPort
    ).withTags(tags.filterNot(tag => TagNames.excludedFromDetail.contains(tag._1)).toArray:_*)
  }
}
