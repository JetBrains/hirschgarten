package org.jetbrains.plugins.bsp.utils

import java.net.URI
import java.net.URLEncoder

/**
 * Utility class for encoding and decoding text using URL encoding, which can be used for creating URI.
 * Directly using the encoder from `java.net` misses the case of space character.
 */
internal object BspURLEncoder {
  /**
   * The method accepts a (maybe malformed) URI, e.g., with spaces, and returns a valid URI string.
   */
  fun encode(uri: String): String {
    return URLEncoder.encode(uri, Charsets.UTF_8)
      // this is a valid character in URI
      .replace("+", "%20")
      // `java.net.URI` cannot parse these 2 characters correctly after encoding
      .replace("%3A", ":")
      .replace("%2F", "/")
      .replace("%25", "%")
  }
}

/**
 * This helper util accepts a (maybe malformed) URI and returns the corresponding URI object
 */
internal fun String.safeCastToURI(): URI = URI.create(BspURLEncoder.encode(this))
