package uk.gov.hmrc.audit.connector

import org.joda.time.DateTime
import org.mockito.InOrder
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import uk.gov.hmrc.audit.AuditResult.{Failure, Rejected, Success}
import uk.gov.hmrc.audit.handler.AuditHandler
import uk.gov.hmrc.audit.serialiser.AuditSerialiserLike
import uk.gov.hmrc.play.audit.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class AuditorSpec extends Specification with BeforeEach with Mockito {
  sequential

  private val SERIAL_VALUE = "serialised_value"

  private var datastreamConnector: AuditHandler = _
  private var loggingConnector: AuditHandler = _
  private var auditSerialiser: AuditSerialiserLike = _

  implicit var order: Option[InOrder] = _
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val auditor = new AuditorImpl
  private val dataEvent = DataEvent("source", "type")
  private val dataCall = DataCall(Map[String, String](), Map[String, String](), DateTime.now())
  private val mergedEvent = MergedDataEvent("source", "type", request = dataCall, response = dataCall)

  def before: Any = {
    datastreamConnector = mock[AuditHandler]
    loggingConnector = mock[AuditHandler]
    auditSerialiser = mock[AuditSerialiserLike]
    order = inOrder(datastreamConnector, loggingConnector, auditSerialiser)
    auditor.datastreamConnector = datastreamConnector
    auditor.loggingConnector = loggingConnector
    auditor.auditSerialiser = auditSerialiser
  }

  "Before sending the audit contents, the Auditor" should {
    "Serialise a DataEvent" in {
      datastreamConnector.sendEvent(SERIAL_VALUE) returns Success
      auditSerialiser.serialise(dataEvent) returns SERIAL_VALUE

      Await.result(auditor.sendEvent(dataEvent), Duration.Inf)

      there was one(auditSerialiser).serialise(dataEvent) andThen
        one(datastreamConnector).sendEvent(SERIAL_VALUE)
    }

    "Serialise a MergedDataEvent" in {
      datastreamConnector.sendEvent(SERIAL_VALUE) returns Success
      auditSerialiser.serialise(mergedEvent) returns SERIAL_VALUE

      auditor.sendMergedEvent(mergedEvent)

      there was one(auditSerialiser).serialise(mergedEvent) andThen
        one(datastreamConnector).sendEvent(SERIAL_VALUE)
    }
  }

  "When sending the audit contents, the Auditor" should {
    "Send an event to the LoggingConnector if the DatastreamConnector call fails" in {
      auditSerialiser.serialise(dataEvent) returns SERIAL_VALUE
      datastreamConnector.sendEvent(SERIAL_VALUE) returns Failure
      loggingConnector.sendEvent(SERIAL_VALUE) returns Success

      val result = auditor.sendEvent(dataEvent)
      Await.result(result, Duration.Inf) must_== Success

      there was one(datastreamConnector).sendEvent(SERIAL_VALUE) andThen
        one(loggingConnector).sendEvent(SERIAL_VALUE)
    }

    "Send an event to the LoggingConnector if the DatastreamConnector call throws any exception" in {
      auditSerialiser.serialise(dataEvent) returns SERIAL_VALUE
      datastreamConnector.sendEvent(SERIAL_VALUE) throws new RuntimeException

      val result = auditor.sendEvent(dataEvent)
      Await.result(result, Duration.Inf) must_== Failure

      there was one(datastreamConnector).sendEvent(SERIAL_VALUE)
      there was no(loggingConnector)
    }

    "Send nothing to the LoggingConnector if the DatastreamConnector call succeeds" in {
      auditSerialiser.serialise(dataEvent) returns SERIAL_VALUE
      datastreamConnector.sendEvent(SERIAL_VALUE) returns Success

      val result = auditor.sendEvent(dataEvent)
      Await.result(result, Duration.Inf) must_== Success

      there was one(datastreamConnector).sendEvent(SERIAL_VALUE)
      there was no(loggingConnector)
    }

    "Abort an event if the DatastreamConnector call rejects it" in {
      auditSerialiser.serialise(dataEvent) returns SERIAL_VALUE
      datastreamConnector.sendEvent(SERIAL_VALUE) returns Rejected
      loggingConnector.sendEvent(SERIAL_VALUE) returns Success

      val result = auditor.sendEvent(dataEvent)
      Await.result(result, Duration.Inf) must_== Rejected

      there was one(datastreamConnector).sendEvent(SERIAL_VALUE)
      there was no(loggingConnector)
    }
  }
}
