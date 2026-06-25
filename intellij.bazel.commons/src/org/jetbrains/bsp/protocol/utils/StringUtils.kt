package org.jetbrains.bsp.protocol.utils

import org.jetbrains.annotations.ApiStatus
import java.security.MessageDigest

@ApiStatus.Internal
object StringUtils {
  private val md5 = ThreadLocal.withInitial { MessageDigest.getInstance ("MD5") }

  /**
   * Creates a md5 hashed string from an original string
   * @param s the original string
   * @param n the number of characters taken from the hashed string
   */
  fun md5Hash(s: String, n: Int): String {
    val md5 = md5.get()
    md5.reset()
    val hash = md5.digest(s.toByteArray())
    return hash.toHexString(0, n / 2 + n % 2).take(n)
  }
}
