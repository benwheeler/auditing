
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
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.audit.connector.AuditConnector
import uk.gov.hmrc.audit.model._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataCall

import scala.concurrent.ExecutionContext

class AuditExamples {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // TODO How is this configured?
  val auditConnector = AuditConnector

  val appName = "some-app"
  val auditType = "AuditType"
  val path = "/some/uri/path?foo=bar&blah=wibble"
  val method: String = "GET"

  def newStyle()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
    // A very simple event with no extra detail fields
    auditConnector.sendEvent(AuditEvent(appName, auditType, method, path))

    // A simple event with a detail field
    auditConnector.sendEvent(AuditEvent(appName, auditType, method, path, detail = Map("myKey" -> "myValue")))

    // A complicated event with a lot of extra information
    // These should only be required if TxM asks for them specifically
    val requestHeaders: Map[String, String] = Map("User-Agent" -> "Foo")
    val identifiers: Map[String, String] = Map("credID" -> "00000001234")
    val enrolments: List[Enrolment] = List(Enrolment("IR-SA", Map("UTR" -> "1234")))
    val responseHeaders: Map[String, String] = Map("Some-Response" -> "value")
    val responseStatus: Int = 403
    // TODO Actually implement this complex example...
    auditConnector.sendEvent(AuditEvent(appName, auditType, method, path, detail = Map("myKey" -> "myValue")))
  }

  def legacyDataEvent()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
    // A legacy DataEvent with tags and details (this will still work)
    val legacyDataEvent = DataEvent(
      auditType = "SomeAuditType",
      tags = hc.toAuditTags("some_transaction_name", path) + ("aTagField" -> "with a tag value"),
      detail = hc.toAuditDetails()
        ++ Map("aDetailField" -> "with a detail value"),
      auditSource = appName)
    auditConnector.sendEvent(legacyDataEvent)

    // The above legacy DataEvent would look like the following with the new code
    auditConnector.sendEvent(AuditEvent(appName, "SomeAuditType", method, path, detail = Map(
      "aDetailField" -> "with a detail value",
      "aTagField" -> "with a tag value"
    )))
  }

  def legacyMergedDataEvent()(implicit hc: HeaderCarrier, auditConnector: AuditConnector): Unit = {
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
    auditConnector.sendEvent(AuditEvent(appName, "SomeAuditType", method, path,
      requestPayload = Some(Payload("application/json", "{\"ohai\": \"gimmeh\"}")),
      responsePayload = Some(Payload("application/json", "{\"icanhaz\": \"kthxbye\"}"))))
  }
}
```

## Deprecations & Breaking Changes

Several things have been deprecated or removed from the original auditing code.

Most notably, the class ```uk.gov.hmrc.play.audit.model.ExtendedDataEvent``` is
no longer supported, as it required JSON classes from Play. If you require nested
values within the ```AuditEvent.detail``` structure, then this is now supported
out of the box without a special class. For example you can pass it a
```Map[String, Map[String, String]]```.

The old classes ```uk.gov.hmrc.play.audit.model.DataEvent``` and
```uk.gov.hmrc.play.audit.model.MergedDataEvent``` are deprecated, but existing
code will still work as normal as this has been re-implemented in a backwards
compatible way *for most use cases*. The exceptions to this are that the methods
```DataEvent.withTags``` and ```DataEvent.withDetail``` which have been removed.
The functionality of these methods is available in the ```DataEvent``` legacy
companion object ```apply``` method, specifically in the ```tags``` and
```detail``` parameters.


## Configuration

**TODO** Add details here


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
