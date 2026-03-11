package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object BazelConnectorFeatureFlags {

  const val ENABLE_LOG: String = "bazel.enable.log"
  private const val KILL_TREE_ON_CANCEL = "bazel.kill.tree.on.cancel"
  private const val KILL_SERVER_ON_CANCEL = "bazel.kill.server.on.cancel"

  val killServerOnCancel: Boolean
    get() = Registry.`is`(KILL_SERVER_ON_CANCEL)

  val killClientTreeOnCancel: Boolean
    get() = Registry.`is`(KILL_TREE_ON_CANCEL)

  val isLogEnabled: Boolean
    get() = Registry.`is`(ENABLE_LOG)
}
