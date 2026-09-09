package org.jetbrains.bazel.clion

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting

@ApiStatus.Internal
object BazelCLionFeatureFlags {

  @VisibleForTesting
  const val CLION_ENABLED: String = "bazel.clion.enable"

  val isCLionEnabled: Boolean get() = isEnabled(CLION_ENABLED)

  private fun isEnabled(key: String): Boolean {
    System.getProperty(key)?.let { value ->
      return value.toBooleanStrict()
    }

    return Registry.`is`(key)
  }
}
