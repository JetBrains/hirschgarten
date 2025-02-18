package org.jetbrains.bazel.config

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bsp.protocol.FeatureFlags

interface BspFeatureFlagsProvider {
  fun getFeatureFlags(): FeatureFlags

  companion object {
    internal val ep = ExtensionPointName.create<BspFeatureFlagsProvider>("org.jetbrains.bsp.bspFeatureFlagsProvider")

    /**
     * retrieve and merge all the [FeatureFlags] objects from the providers
     */
    fun accumulateFeatureFlags(): FeatureFlags =
      if (ep.extensionList.isEmpty()) {
        FeatureFlags()
      } else {
        ep.extensionList.map { it.getFeatureFlags() }.reduce { acc, fetcher -> acc.merge(fetcher) }
      }
  }
}
