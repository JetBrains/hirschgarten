package org.jetbrains.plugins.bsp.utils

import java.net.URI

/**
 * Utility class for encoding and decoding text using URL encoding, which can be used for creating URI.
 * Directly using the encoder from `java.net` misses the case of space character.
 */
internal object BspURLEncoder {
  /**
   * The method accepts a (maybe malformed) URI, e.g., with spaces, and returns an encoded valid URI string.
   */
  fun encode(uri: String): String {
    return java.net.URLEncoder.encode(uri, Charsets.UTF_8)
      // this is a valid character in URI
      .replace("+", "%20")
      // `java.net.URI` cannot parse these 2 characters correctly after encoding
      .replace("%3A", ":")
      .replace("%2F", "/")
  }
}

/**
 * This helper util accepts a (maybe malformed) URI and returns the corresponding URI object
 */
internal fun String.safeCastToURI() = URI.create(BspURLEncoder.encode(this))
