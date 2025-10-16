package org.jetbrains.bazel.workspacecontext.provider

import com.google.common.hash.HashCode
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.attribute.FileTime

interface WorkspaceContextProvider {
  /**
   * Prefer to pass [WorkspaceContext] as a parameter where possible instead of calling this method to avoid reparsing the project view.
   */
  fun readWorkspaceContext(): WorkspaceContext

  fun currentFeatureFlags(): FeatureFlags

  fun getConfigurationHash(): HashCode
}
