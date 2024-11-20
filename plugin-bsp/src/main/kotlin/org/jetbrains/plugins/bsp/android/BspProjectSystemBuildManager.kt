package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildMode
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildResult
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildStatus
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspaceListener

class BspProjectSystemBuildManager(private val project: Project) : ProjectSystemBuildManager {
  @Deprecated("Do not add new uses of this method as it's error prone")
  override val isBuilding: Boolean
    get() = false

  private var lastBuildResult: BuildResult = BuildResult.createUnknownBuildResult()

  override fun getLastBuildResult(): BuildResult = lastBuildResult

  override fun addBuildListener(parentDisposable: Disposable, buildListener: ProjectSystemBuildManager.BuildListener) {
    project.messageBus.connect(parentDisposable).subscribe(
      BspWorkspaceListener.TOPIC,
      object : BspWorkspaceListener {
        override fun syncStarted() {
          buildListener.buildStarted(BuildMode.COMPILE_OR_ASSEMBLE)
        }

        override fun syncFinished(canceled: Boolean) {
          val status = if (canceled) BuildStatus.CANCELLED else BuildStatus.SUCCESS
          buildListener.buildCompleted(BuildResult(BuildMode.COMPILE_OR_ASSEMBLE, status))
        }
      },
    )
  }

  override fun compileProject() {
    BspCoroutineService.getInstance(project).start {
      ProjectSyncTask(project).sync(syncScope = FullProjectSync, buildProject = true)
    }
  }
}
