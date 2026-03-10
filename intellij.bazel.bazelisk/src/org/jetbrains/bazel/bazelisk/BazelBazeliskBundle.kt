package org.jetbrains.bazel.bazelisk

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

internal object BazelBazeliskBundle {
  const val BUNDLE = "messages.BazelBazeliskBundle"
  private val INSTANCE = DynamicBundle(BazelBazeliskBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
