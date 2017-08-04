package uk.gov.hmrc.audit.model

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.model.DataCall
import uk.gov.hmrc.audit.model.AuditEvent._
import uk.gov.hmrc.time.DateTimeUtils

object TagNames {
  val path: String = "path"
  val clientIP: String = "clientIP"
  val clientPort: String = "clientPort"
  val transactionName: String = "transactionName"
  val requestID: String = HeaderNames.xRequestId
  val sessionID: String = HeaderNames.xSessionId
  val authorisation: String = HeaderNames.authorisation

  val excludedFromDetail = Seq(path, clientIP, clientPort, transactionName, requestID, sessionID, authorisation)
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

    import TagNames._

    AuditEvent(auditSource,
      auditType,
      "",
      tags.getOrElse(path, ""),
      generatedAt,
      detail ++ tags.filter(tag => !excludedFromDetail.contains(tag._1) && nonEmpty(tag)),
      eventID = eventId,
      requestID = getString(tags, requestID),
      sessionID = getString(tags, sessionID),
      authorisationToken = getString(tags, authorisation),
      clientIP = getString(tags, clientIP),
      clientPort = getString(tags, clientPort).flatMap(value => Some(value.toInt))
    )
  }
}
