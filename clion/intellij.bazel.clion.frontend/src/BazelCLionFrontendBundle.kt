package org.jetbrains.bazel.clion.frontend

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

internal object BazelCLionFrontendBundle {

  const val BUNDLE_FQN: @NonNls String = "messages.BazelCLionFrontendBundle"

  private val BUNDLE = DynamicBundle(BazelCLionFrontendBundle::class.java, BUNDLE_FQN)

  fun message(
    @NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String,
    vararg params: Any,
  ): @Nls String = BUNDLE.getMessage(key, *params)
}
