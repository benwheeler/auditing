package uk.gov.hmrc.audit

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.audit.connector.AuditConnector
import uk.gov.hmrc.audit.model.{AuditEvent, Enrolment, Payload}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import scala.concurrent.{ExecutionContext, Future}

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

  def send(event: AuditEvent)(implicit auditConnector: AuditConnector, ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendEvent(event)
  }

  def auditEvent(auditSource: String, auditType: String, method: String,
    pathString: String, detail: Map[String, _] = Map(), identifiers: Option[Map[String, String]] = None,
    enrolments: Option[List[Enrolment]] = None, requestHeaders: Option[Map[String, String]] = None,
    requestPayload: Option[Payload] = None, responseHeaders: Option[Map[String, String]] = None,
    responsePayload: Option[Payload] = None, responseStatus: Option[Int] = None,
    clientIP: Option[String] = None, clientPort: Option[Int] = None
  )(implicit hc: HeaderCarrier): AuditEvent = {

    if (hc.requestId.getOrElse("").equals("-")) throw new IllegalArgumentException("Request ID must be defined.")

    val pathTuple = AuditEvent.splitPath(pathString)

    AuditEvent(auditSource,
      auditType,
      now,
      newUUID,
      hc.requestId.get.value,
      hc.sessionId.flatMap(sessionId => Some(sessionId.value)),
      pathTuple._1,
      method,
      Option(pathTuple._2).filter(_.trim.nonEmpty),
      clientIP.orElse(hc.trueClientIp.orElse(None)),
      clientPort.orElse(hc.trueClientPort.flatMap(value => Some(value.toInt))),
      hc.authorization.flatMap(authorisation => Some(authorisation.value)),
      collectRequestHeaders(requestHeaders.getOrElse(Map()), hc),
      requestPayload,
      collectIdentifiers(detail, identifiers.getOrElse(Map())),
      enrolments,
      collectDetailWithoutIds(detail),
      responseHeaders,
      responseStatus,
      responsePayload
    )
    /*
     userId: Option[UserId] = None,
     token: Option[Token] = None,
     nsStamp: Long = System.nanoTime(),
     trueClientIp: Option[String] = None,
     trueClientPort: Option[String] = None,
     gaToken: Option[String] = None,
     gaUserId: Option[String] = None,
     deviceID: Option[String] = None,
     */
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
}

object AuditExtensions {
  val excludedRequestHeaders: Seq[String] = Seq(
    // add any header exclusions here
  )

  val detailsToIdentifiers: Map[String, String] = Map(
    "credId" -> "credID"
  )
}
