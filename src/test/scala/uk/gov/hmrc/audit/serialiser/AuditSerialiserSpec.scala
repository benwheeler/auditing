package uk.gov.hmrc.audit.serialiser

import java.util.UUID

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import uk.gov.hmrc.play.audit.model.{DataCall, DataEvent, ExtendedDataEvent, MergedDataEvent}

class AuditSerialiserSpec extends Specification {

  val serialiser = new AuditSerialiser
  val uuid: String = UUID.randomUUID().toString
  val dateTime: DateTime = DateTime.parse("2017-01-01T12:00+00:00")
  val dateString: String = "2017-01-01T12:00:00.000Z"

  "When serialising a DataEvent the result" should {
    "Populate all supplied fields in the correct format" in {
      val dataEvent = DataEvent("source", "type", uuid, generatedAt = dateTime).withTags(("foo", "bar")).withDetail(("one", "two"))
      val expectedResult = s"""{"auditSource":"source","auditType":"type","eventId":"$uuid","tags":{"foo":"bar"},"detail":{"one":"two"},"generatedAt":"$dateString"}"""
      serialiser.serialise(dataEvent) must be equalTo expectedResult
    }

    "Omit any empty fields" in {
      val dataEvent = DataEvent("source", "type", uuid, generatedAt = dateTime).withTags(("foo", "bar"), ("blah", null)).withDetail(("one", "two"), ("three", null))
      val expectedResult = s"""{"auditSource":"source","auditType":"type","eventId":"$uuid","tags":{"foo":"bar"},"detail":{"one":"two"},"generatedAt":"$dateString"}"""
      serialiser.serialise(dataEvent) must be equalTo expectedResult
    }

    "Omit any objects that have no fields" in {
      val dataEvent = DataEvent("source", "type", uuid, generatedAt = dateTime)
      val expectedResult = s"""{"auditSource":"source","auditType":"type","eventId":"$uuid","generatedAt":"$dateString"}"""
      serialiser.serialise(dataEvent) must be equalTo expectedResult
    }
  }

  "When serialising a MergedDataEvent the result" should {
    "Populate all supplied fields in the correct format" in {
      val requestDataCall = DataCall(Map[String, String](("foo", "bar")), Map[String, String](("one", "two")), dateTime)
      val responseDataCall = DataCall(Map[String, String](("blah", "baz")), Map[String, String](("three", "four")), dateTime)
      val mergedEvent = MergedDataEvent("source", "type", uuid, requestDataCall, responseDataCall)
      val expectedResult = s"""{"auditSource":"source","auditType":"type","eventId":"$uuid","request":{"tags":{"foo":"bar"},"detail":{"one":"two"},"generatedAt":"$dateString"},"response":{"tags":{"blah":"baz"},"detail":{"three":"four"},"generatedAt":"$dateString"}"""
      serialiser.serialise(mergedEvent) must be equalTo expectedResult
    }
  }

  "When serialising a ExtendedDataEvent the result" should {
    "Populate all supplied fields in the correct format" in {
      val dataEvent = ExtendedDataEvent("source", "type")
      val expectedResult = s"""{"auditSource":"source","auditType":"type","eventId":"$uuid","tags":{},"detail":{"value":""},"generatedAt":"$dateString"}"""
      serialiser.serialise(dataEvent) must be equalTo expectedResult
    }
  }
}
