/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.audit.connector

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.audit.AuditResult
import uk.gov.hmrc.audit.handler.{AuditHandler, DatastreamHandler, LoggingHandler}
import uk.gov.hmrc.audit.model.AuditEvent
import uk.gov.hmrc.audit.serialiser.{AuditSerialiser, AuditSerialiserLike}
import uk.gov.hmrc.play.audit.model.{DataEvent, MergedDataEvent}

import scala.concurrent.{ExecutionContext, Future}

trait AuditConnector {
  def sendEvent(event: AuditEvent)(implicit ec: ExecutionContext): Future[AuditResult]

  @deprecated("Use sendEvent(AuditEvent) for new code")
  def sendEvent(event: DataEvent)(implicit ec: ExecutionContext): Future[AuditResult]

  @deprecated("Use sendEvent(AuditEvent) for new code")
  def sendMergedEvent(event: MergedDataEvent)(implicit ec: ExecutionContext): Future[AuditResult]
}

class AuditorImpl extends AuditConnector {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  var datastreamConnector: AuditHandler = DatastreamHandler
  var loggingConnector: AuditHandler = LoggingHandler
  var auditSerialiser: AuditSerialiserLike = AuditSerialiser

  def sendEvent(event: AuditEvent)(implicit ec: ExecutionContext): Future[AuditResult] = {
    send(auditSerialiser.serialise(event))
  }

  @deprecated
  def sendEvent(event: DataEvent)(implicit ec: ExecutionContext): Future[AuditResult] = {
    send(auditSerialiser.serialise(event))
  }

  @deprecated
  def sendMergedEvent(event: MergedDataEvent)(implicit ec: ExecutionContext): Future[AuditResult] = {
    send(auditSerialiser.serialise(event))
  }

  private def send(event: String)(implicit ec: ExecutionContext): Future[AuditResult] = Future {
    try {
      val result: AuditResult = datastreamConnector.sendEvent(event)
      result match {
        case AuditResult.Success =>
          result
        case AuditResult.Rejected =>
          result
        case AuditResult.Failure =>
          loggingConnector.sendEvent(event)
      }
    } catch {
      case e: Throwable =>
        log.error("Error in handler code", e)
        AuditResult.Failure
    }
  }
}
