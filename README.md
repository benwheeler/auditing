
# auditing

[![Build Status](https://travis-ci.org/hmrc/auditing.svg?branch=master)](https://travis-ci.org/hmrc/auditing) [ ![Download](https://api.bintray.com/packages/hmrc/releases/auditing/images/download.svg) ](https://bintray.com/hmrc/releases/auditing/_latestVersion)

This library contains code to facilitate creation of audit events and their publication to datastream.


## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "auditing" % "x.x.x"
```

## Usage

```scala
import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.audit.AuditExtensions
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.audit.connector.AuditConnector
import uk.gov.hmrc.audit.model.{DataEvent, Enrolment, MergedDataEvent, Payload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataCall

class AuditExtensionsExamples extends AuditExtensions {

  // TODO How is this configured?
  implicit val auditConnector = AuditConnector

  val appName = "some-app"
  val auditType = "AuditType"
  val path = "/some/uri/path?foo=bar&blah=wibble"
  val method: String = "GET"

  def newStyleExamples()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
    // A very simple event with no extra detail fields
    send(auditEvent(appName, auditType, method, path))

    // A simple event with a detail field
    send(auditEvent(appName, auditType, method, path, Map("myKey" -> "myValue")))

    // A complicated event with a lot of extra information
    // These should only be required if TxM asks for them specifically
    val requestHeaders: Map[String, String] = Map("User-Agent" -> "Foo")
    val identifiers: Map[String, String] = Map("credID" -> "00000001234")
    val enrolments: List[Enrolment] = List(Enrolment("IR-SA", Map("UTR" -> "1234")))
    val responseHeaders: Map[String, String] = Map("Some-Response" -> "value")
    val responseStatus: Int = 403
    send(auditEvent(appName, auditType, method, path, Map("myKey" -> "myValue")))
  }

  def legacyDataEventExample()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
    // A legacy DataEvent with tags and details (this will still work)
    val legacyDataEvent = DataEvent(
      auditType = "SomeAuditType",
      tags = hc.toAuditTags("some_transaction_name", path),
      detail = hc.toAuditDetails()
        ++ Map("aDetailField" -> "with a detail value"),
      auditSource = appName).withTags("aTagField" -> "with a tag value")
    auditConnector.sendEvent(legacyDataEvent)

    // The above legacy DataEvent would look like the following with the new code
    send(auditEvent(appName, "SomeAuditType", method, path, Map(
      "aDetailField" -> "with a detail value",
      "aTagField" -> "with a tag value"
    )))
  }

  def legacyMergedDataEventExample()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
    // A legacy MergedDataEvent with tags and details (this will still work)
    val legacyMergedDataEvent = MergedDataEvent(
      auditSource = appName,
      auditType = "SomeAuditType",
      request = DataCall(
        tags = hc.toAuditTags("some_transaction_name", path),
        detail = Map("requestBody" -> "{\"ohai\": \"gimmeh\"}"),
        generatedAt = DateTime.now(DateTimeZone.UTC)
      ),
      response = DataCall(
        tags = hc.toAuditTags("some_transaction_name", path),
        detail = Map("responseBody" -> "{\"icanhaz\": \"kthxbye\"}"),
        generatedAt = DateTime.now(DateTimeZone.UTC)
      )
    )
    auditConnector.sendMergedEvent(legacyMergedDataEvent)

    // The above legacy MergedDataEvent would look like the following with the new code
    send(auditEvent(appName, "SomeAuditType", method, path,
      requestPayload = Some(Payload("{\"ohai\": \"gimmeh\"}", "application/json")),
      responsePayload = Some(Payload("{\"icanhaz\": \"kthxbye\"}", "application/json"))))
  }
}
```

## Configuration

**TODO** Add details here


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
