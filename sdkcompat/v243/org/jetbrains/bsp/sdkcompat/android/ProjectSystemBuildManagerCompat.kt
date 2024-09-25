package org.jetbrains.bsp.sdkcompat.android

import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.intellij.openapi.vfs.VirtualFile

// v243: Removed compileFilesAndDependencies
abstract class ProjectSystemBuildManagerCompat : ProjectSystemBuildManager {
  abstract fun compileFilesAndDependenciesCompat(files: Collection<VirtualFile>): Unit
}
