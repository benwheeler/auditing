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

package uk.gov.hmrc.audit.serialiser

import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.json4s.prefs.EmptyValueStrategy
import org.json4s.{Formats, JArray, JField, JNothing, JNull, JObject, JValue, NoTypeHints}
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.audit.model.AuditEvent
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent, MergedDataEvent}

trait AuditSerialiserLike {
  def serialise(event: DataEvent): String
  def serialise(event: MergedDataEvent): String
  def serialise(event: ExtendedDataEvent): String
  def serialise(event: AuditEvent): String
}

// TODO An attempt to remove the empty JSON objects; sadly this doesn't work
class CustomEmptyValueStrategy extends EmptyValueStrategy {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  override def noneValReplacement: Option[AnyRef] = None

  override def replaceEmpty(value: JValue): JValue = {
    log.info(s"replaceEmpty : ${value.getClass}")

    value match {
      case JArray(items) =>
        log.info(s"Array with item count : ${items.size}")
        JArray(items map replaceEmpty)
      case JObject(fields) =>
        log.info(s"Object with field count : ${fields.size}")
        JObject(fields map {
          case JField(name, value) => JField(name, replaceEmpty(value))
        })
      case JNothing => JNull
      case oth => oth
    }
  }
}

class AuditSerialiser extends AuditSerialiserLike {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  implicit val formats: Formats = Serialization.formats(NoTypeHints).skippingEmptyValues ++
    JodaTimeSerializers.all

  override def serialise(event: DataEvent): String = {
    log.info(s"Serialise a DataEvent")
    write(event)
  }

  override def serialise(event: MergedDataEvent): String = {
    log.info(s"Serialise a MergedDataEvent")
    write(event)
  }

  override def serialise(event: ExtendedDataEvent): String = {
    log.info(s"Serialise an ExtendedDataEvent")
    write(event)
  }

  override def serialise(event: AuditEvent): String = {
    log.info(s"Serialise an AuditEvent")
    write(event)
  }
}

object AuditSerialiser extends AuditSerialiser
