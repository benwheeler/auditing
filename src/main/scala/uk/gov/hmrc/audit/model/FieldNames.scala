package uk.gov.hmrc.audit.model

object FieldNames {
  /*
   * Determines whether request headers should be in "clientHeaders" (if not
   * prefixed by a value in this list) or "requestHeaders" (if matching a prefix
   * in this list).
   */
  val requestHeaderPrefixes = Seq("X-", "Forwarded", "Akamai-", "Via", "Surrogate")

  /*
   * Any headers that should never be recorded.
   */
  val excludedRequestHeaders: Seq[String] = Seq(
  )

  /*
   * Rewrites "detail" entries (where the name matches this map key) into the
   * "identifiers" collection (where the name is taken from this map value).
   */
  val detailsToIdentifiers: Map[String, String] = Map(
    "credId" -> "credID"
  )

  /*
   * Detail fields that are handled in a special way for backward compatibility.
   */
  object LegacyDetailNames {
    val akamaiReputation = "Akamai-Reputation" // Needed?

    val method = "method"
    val statusCode = "statusCode"
    val requestBody = "requestBody"
    val responseMessage = "responseMessage"
    val queryString = "queryString"
    val referrer = "referrer"
    val authorisation = "Authorization"
  }
}
