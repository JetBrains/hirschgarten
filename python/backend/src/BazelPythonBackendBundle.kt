package com.intellij.bazel.python.backend

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

internal object BazelPythonBackendBundle {
  const val BUNDLE = "messages.BazelPythonBackendBundle"
  private val INSTANCE = DynamicBundle(BazelPythonBackendBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
