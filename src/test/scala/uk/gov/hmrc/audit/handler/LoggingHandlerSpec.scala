package uk.gov.hmrc.audit.handler

import org.slf4j.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class LoggingHandlerSpec extends Specification with Mockito {

  val mockLog: Logger = mock[Logger]
  val loggingHandler = new LoggingHandler(mockLog)

  "When logging an error, the message" should {
    "Start with a known value so that downstream processing knows how to deal with it" in {
      val expectedLogContent = "DS_EventMissed_AuditRequestFailure : audit item : FAILED_EVENT"

      loggingHandler.sendEvent("FAILED_EVENT")

      there was one(mockLog).warn(expectedLogContent)
    }
  }
}
