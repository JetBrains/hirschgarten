package org.jetbrains.bazel.sync_new

import com.intellij.openapi.util.registry.Registry

object BazelSyncV2 {
  val isEnabled: Boolean
    get() = Registry.`is`("bazel.new.sync.enabled")

  // TODO: add settings/feature flag
  val useOptimizedInverseSourceQuery: Boolean = true
  val useSkyQueryForInverseSourceQueries: Boolean = true

  val useFastSource2Label: Boolean = true

  val useNewTargetTreeStorage: Boolean = true

  val useFileChangeBasedInvalidation: Boolean = true
}
