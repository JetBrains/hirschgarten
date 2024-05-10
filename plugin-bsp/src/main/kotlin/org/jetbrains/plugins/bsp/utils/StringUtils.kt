package org.jetbrains.plugins.bsp.utils

import java.security.MessageDigest

internal object StringUtils {
  private val MD5 = MessageDigest.getInstance("MD5")

  /**
   * Creates a md5 hashed string from an original string
   * @param s the original string
   * @param n the number of characters taken from the hashed string
   */
  @OptIn(ExperimentalStdlibApi::class)
  fun md5Hash(s: String, n: Int): String {
    val hash = MD5.digest(s.toByteArray())
    return hash.toHexString(0, n / 2 + n % 2).take(n)
  }
}
