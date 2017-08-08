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

import org.joda.time.DateTime

@deprecated("Use uk.gov.hmrc.audit.model.AuditEvent instead.")
case class DataEvent(auditSource: String,
                     auditType: String,
                     eventId: String,
                     tags: Map[String, String],
                     detail: Map[String, String],
                     generatedAt: DateTime)

@deprecated("Support for this type has been removed due to Play dependency.")
case class ExtendedDataEvent(auditSource: String,
                             auditType: String,
                             eventId: String,
                             tags: Map[String, String],
                             detail: String = "",
                             generatedAt: DateTime)

@deprecated("Use uk.gov.hmrc.audit.model.AuditEvent instead.")
case class DataCall(tags: Map[String, String],
                    detail: Map[String, String],
                    generatedAt: DateTime)

@deprecated("Use uk.gov.hmrc.audit.model.AuditEvent instead.")
case class MergedDataEvent(auditSource: String,
                           auditType: String,
                           eventId: String,
                           request: DataCall,
                           response: DataCall)
