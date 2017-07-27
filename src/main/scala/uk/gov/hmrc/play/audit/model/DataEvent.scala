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

package uk.gov.hmrc.play.audit.model

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.time.DateTimeUtils

@deprecated
sealed trait AuditEvent {
  def auditSource: String
  def auditType: String
  def eventId: String
  def tags: Map[String, String]
  def generatedAt: DateTime
}

@deprecated("Use class uk.gov.hmrc.audit.model.DataEvent for legacy code. " +
  "Use uk.gov.hmrc.audit.model.AuditEvent for new code.")
case class DataEvent(override val auditSource: String,
                     override val auditType: String,
                     override val eventId: String = UUID.randomUUID().toString,
                     override val tags: Map[String, String] = Map.empty,
                     detail: Map[String, String] = Map.empty,
                     override val generatedAt: DateTime = DateTimeUtils.now) extends AuditEvent {

  def withDetail(moreDetail: (String, String)*): DataEvent = copy(detail = detail ++ moreDetail)

  def withTags(moreTags: (String, String)*): DataEvent = copy(tags = tags ++ moreTags)
}

@deprecated
case class ExtendedDataEvent(override val auditSource: String,
                             override val auditType: String,
                             override val eventId: String = UUID.randomUUID().toString,
                             override val tags: Map[String, String] = Map.empty,
                             detail: String = "",
                             override val generatedAt: DateTime = DateTimeUtils.now) extends AuditEvent

@deprecated
case class DataCall(tags: Map[String, String],
                    detail: Map[String, String],
                    generatedAt: DateTime)

@deprecated
case class MergedDataEvent(auditSource: String,
                           auditType: String,
                           eventId: String = UUID.randomUUID().toString,
                           request: DataCall,
                           response: DataCall)
