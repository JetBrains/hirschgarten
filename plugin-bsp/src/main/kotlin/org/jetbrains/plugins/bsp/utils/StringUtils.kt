package org.jetbrains.plugins.bsp.utils

import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

internal object StringUtils {
  private val DIGESTERS = mapOf(
    "MD5" to MessageDigest.getInstance("MD5")
  )

  /**
   * Creates a md5 hashed string from an original string
   * @param s the original string
   * @param n the number of characters taken from the hashed string
   */
  fun md5Hash(s: String, n: Int): String = hash(s, n, "MD5")

  /**
   * Creates a hashed string from an original string
   * @param s the original string
   * @param n the number of characters taken from the hashed string
   * @param algorithm the algorithm to use for hashing the string
   */
  private fun hash(s: String, n: Int, algorithm: String): String {
    val md = DIGESTERS[algorithm] ?: error("Unsupported algorithm")
    val hash = md.digest(s.toByteArray())
    return hash.toHexString().substring(0, n)
  }
}
