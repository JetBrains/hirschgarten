package org.jetbrains.bazel.sync_new

import com.intellij.openapi.util.registry.Registry

object BazelNewSyncFlow {
  val isEnabled: Boolean
    get() = Registry.`is`("bazel.new.sync.enabled")
}
