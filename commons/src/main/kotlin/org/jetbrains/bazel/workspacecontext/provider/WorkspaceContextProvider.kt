package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags

interface WorkspaceContextProvider {
  /**
   * Prefer to pass [WorkspaceContext] as a parameter where possible instead of calling this method to avoid reparsing the project view.
   */
  fun readWorkspaceContext(): WorkspaceContext

  fun currentFeatureFlags(): FeatureFlags
}
