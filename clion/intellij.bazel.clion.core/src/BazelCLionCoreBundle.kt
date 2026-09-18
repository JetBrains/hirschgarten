package org.jetbrains.bazel.clion

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

internal object BazelCLionCoreBundle {

  const val BUNDLE_FQN: @NonNls String = "messages.BazelCLionCoreBundle"

  private val BUNDLE = DynamicBundle(BazelCLionCoreBundle::class.java, BUNDLE_FQN)

  fun message(
    @NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String,
    vararg params: Any,
  ): @Nls String = BUNDLE.getMessage(key, *params)
}
