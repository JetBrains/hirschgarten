package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile

public class BspProjectSystemBuildManager : ProjectSystemBuildManager {
  @Suppress("OVERRIDE_DEPRECATION")
  override val isBuilding: Boolean
    get() = false

  override fun addBuildListener(parentDisposable: Disposable, buildListener: ProjectSystemBuildManager.BuildListener) {}

  override fun compileFilesAndDependencies(files: Collection<VirtualFile>) {}

  override fun compileProject() {}

  override fun getLastBuildResult(): ProjectSystemBuildManager.BuildResult =
    ProjectSystemBuildManager.BuildResult.createUnknownBuildResult()
}
