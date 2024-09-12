package org.jetbrains.bsp.sdkcompat.android

import com.android.tools.idea.projectsystem.ProjectSystemBuildManager

// v243: Dropped a parameter
fun buildResult(
  buildMode: ProjectSystemBuildManager.BuildMode,
  status: ProjectSystemBuildManager.BuildStatus,
): ProjectSystemBuildManager.BuildResult = ProjectSystemBuildManager.BuildResult(buildMode, status)
