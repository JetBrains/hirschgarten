package org.jetbrains.bsp.sdkcompat.android

// v243: Removed compileFilesAndDependencies
abstract class ProjectSystemBuildManagerCompat : com.android.tools.idea.projectsystem.ProjectSystemBuildManager {
  abstract fun compileFilesAndDependenciesCompat(files: Collection<com.intellij.openapi.vfs.VirtualFile>): Unit

  override fun compileFilesAndDependencies(files: Collection<com.intellij.openapi.vfs.VirtualFile>) {
    compileFilesAndDependenciesCompat(files)
  }
}
