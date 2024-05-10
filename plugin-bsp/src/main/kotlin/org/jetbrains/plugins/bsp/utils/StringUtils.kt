package org.jetbrains.plugins.bsp.utils

import java.security.MessageDigest

internal object StringUtils {
  /**
   * Creates a md5 hashed string from an original string
   * @param s the original string
   * @param n the number of characters taken from the hashed string
   */
  @OptIn(ExperimentalStdlibApi::class)
  fun md5Hash(s: String, n: Int): String {
    val hash = MessageDigest.getInstance("MD5".intern()).digest(s.toByteArray())
    return hash.toHexString(0, n / 2 + n % 2).take(n)
  }
}
