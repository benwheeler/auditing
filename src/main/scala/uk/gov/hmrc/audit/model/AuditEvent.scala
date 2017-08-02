package uk.gov.hmrc.audit.model

import java.net.URI
import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.audit.AuditExtensions
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

/**
  * Class used for all audit events.
  *
  * This is a replacement for all the uk.gov.hmrc.play.audit.model case classes.
  */
class AuditEvent private (
  val auditSource: String,
  val auditType: String,
  val generatedAt: DateTime,
  val eventID: String,
  val requestID: String,
  var sessionID: Option[String],
  val path: String,
  val method: String,
  val queryString: Option[String],
  val clientIP: Option[String],
  val clientPort: Option[Int],
  val authorisationToken: Option[String],
  val requestHeaders: Option[Map[String, String]],
  val requestPayload: Option[Payload],
  val identifiers: Option[Map[String, String]],
  val enrolments: Option[List[Enrolment]],
  var detail: Option[Map[String, _]],
  val responseHeaders: Option[Map[String, String]],
  val responseStatus: Option[Int],
  val responsePayload: Option[Payload],
  val version: Int) {

  require(requestID != null && !requestID.trim.isEmpty && !requestID.trim.equals("-"))
  require(eventID != null && !eventID.trim.isEmpty && !eventID.trim.equals("-"))
  require(generatedAt != null)

  @deprecated("Use the constructor instead of this method")
  def withDetail(moreDetail: (String, String)*): AuditEvent = {
    detail = Some(detail.getOrElse(Map[String, AnyVal]()) ++ moreDetail)
    this
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
  def apply(auditSource: String,
    auditType: String,
    method: String,
    pathString: String,
    generatedAt: DateTime = DateTime.now,
    detail: Map[String, _] = Map(),
    identifiers: Option[Map[String, String]] = None,
    enrolments: Option[List[Enrolment]] = None,
    requestHeaders: Option[Map[String, String]] = None,
    requestPayload: Option[Payload] = None,
    responseHeaders: Option[Map[String, String]] = None,
    responsePayload: Option[Payload] = None,
    responseStatus: Option[Int] = None,
    clientIP: Option[String] = None,
    clientPort: Option[Int] = None,
    requestID: Option[String] = None,
    sessionID: Option[String] = None,
    authorisationToken: Option[String] = None,
    version: Int = 1,
    eventID: String = newUUID
  )(implicit hc: HeaderCarrier): AuditEvent = {

    val pathTuple = splitPath(pathString)

    new AuditEvent(auditSource,
      auditType,
      generatedAt,
      eventID,
      requestID.getOrElse(hc.requestId.get.value),
      sessionID.orElse(hc.sessionId.flatMap(sessionId => Some(sessionId.value))),
      pathTuple._1,
      method,
      Option(pathTuple._2).filter(_.trim.nonEmpty),
      clientIP.orElse(hc.trueClientIp.orElse(None)),
      clientPort.orElse(hc.trueClientPort.flatMap(value => Some(value.toInt))),
      authorisationToken.orElse(hc.authorization.flatMap(authorisation => Some(authorisation.value))),
      collectRequestHeaders(requestHeaders.getOrElse(Map()), hc),
      requestPayload,
      collectIdentifiers(detail, identifiers.getOrElse(Map())),
      enrolments,
      collectDetailWithoutIds(detail),
      responseHeaders,
      responseStatus,
      responsePayload,
      version
    )
  }

  private def collectIdentifiers(detail: Map[String, _], identifiers: Map[String, String]): Option[Map[String, String]] = {
    val filteredIds = identifiers.filter(p => {
      p._1.nonEmpty && nonEmpty(p._2)
    }) ++ collectIdsFromDetail(detail)

    if (filteredIds.isEmpty) None else Some(filteredIds)
  }

  private def collectIdsFromDetail(detail: Map[String, _]): Map[String, String] = {
    detail.flatMap(p => {
      if (AuditExtensions.detailsToIdentifiers.contains(p._1) && p._2.isInstanceOf[String]) {
        val stringValue = p._2.asInstanceOf[String]
        if (nonEmpty(stringValue)) {
          Some((AuditExtensions.detailsToIdentifiers(p._1), stringValue))
        } else {
          None
        }
      } else {
        None
      }
    })
  }

  private def collectDetailWithoutIds(detail: Map[String, _]): Option[Map[String, _]] = {
    val filteredDetail = detail.filter(p => {
      p._1.nonEmpty && !AuditExtensions.detailsToIdentifiers.contains(p._1) &&
        (
          (p._2.isInstanceOf[String] && nonEmpty(p._2.asInstanceOf[String])) ||
            (p._2.isInstanceOf[Map[_, _]] && p._2.asInstanceOf[Map[_, _]].nonEmpty)
          )
    })

    if (filteredDetail.isEmpty) None else Some(filteredDetail)
  }

  private def collectRequestHeaders(suppliedRequestHeaders: Map[String, String], hc: HeaderCarrier): Option[Map[String, String]] = {
    val headers = (suppliedRequestHeaders ++ hc.extraHeaders ++ hc.otherHeaders ++
      hc.forwarded.map(f => HeaderNames.xForwardedFor -> f.value) ++
      hc.akamaiReputation.map(f => HeaderNames.akamaiReputation -> f.value)
      ).filter(p => {
      p._1.nonEmpty && !AuditExtensions.excludedRequestHeaders.contains(p._1) &&
        p._2.nonEmpty && !p._2.trim.equals("-")
    })

    if (headers.isEmpty) None else Some(headers)
  }

  private def nonEmpty(value: String): Boolean = {
    value.trim match {
      case "" => false
      case "-" => false
      case _ => true
    }
  }

  def now: DateTime = {
    DateTime.now()
  }

  def newUUID: String = {
    UUID.randomUUID.toString
  }

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
