package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

internal object BazelProjectViewBundle {
  const val BUNDLE = "messages.BazelProjectViewBundle"
  private val INSTANCE = DynamicBundle(BazelProjectViewBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
