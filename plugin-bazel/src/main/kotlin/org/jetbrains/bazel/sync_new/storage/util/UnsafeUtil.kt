package org.jetbrains.bazel.sync_new.storage.util

import sun.misc.Unsafe

object UnsafeUtil {
  val unsafe: Unsafe by lazy {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    field.get(null) as Unsafe
  }
}
