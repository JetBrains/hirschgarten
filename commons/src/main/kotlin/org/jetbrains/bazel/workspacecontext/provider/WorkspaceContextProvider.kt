package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.attribute.FileTime

interface WorkspaceContextProvider {
  /**
   * Prefer to pass [WorkspaceContext] as a parameter where possible instead of calling this method to avoid reparsing the project view.
   */
  fun readWorkspaceContext(): WorkspaceContext

  fun currentFeatureFlags(): FeatureFlags

  /**
   * Returns the modification time of the workspace configuration file for change detection.
   * Used to determine when the server needs to be restarted due to configuration changes.
   */
  fun getConfigurationModificationTime(): FileTime?
}
