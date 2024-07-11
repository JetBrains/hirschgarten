package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildMode
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildResult
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildStatus
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.config.BspWorkspaceListener
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService

public class BspProjectSystemBuildManager(private val project: Project) : ProjectSystemBuildManager {
  @Deprecated("Do not add new uses of this method as it's error prone")
  override val isBuilding: Boolean
    get() = false

  private var lastBuildResult: BuildResult = BuildResult.createUnknownBuildResult()

  override fun getLastBuildResult(): BuildResult = lastBuildResult

  override fun addBuildListener(parentDisposable: Disposable, buildListener: ProjectSystemBuildManager.BuildListener) {
    project.messageBus.connect(parentDisposable).subscribe(BspWorkspaceListener.TOPIC, object : BspWorkspaceListener {
      override fun syncStarted() {
        buildListener.buildStarted(BuildMode.COMPILE)
      }

      override fun syncFinished(canceled: Boolean) {
        val status = if (canceled) BuildStatus.CANCELLED else BuildStatus.SUCCESS
        buildListener.buildCompleted(BuildResult(BuildMode.COMPILE, status, System.currentTimeMillis()))
      }
    })
  }

  override fun compileProject() {
    BspCoroutineService.getInstance(project).start {
      SyncProjectTask(project).execute(
        shouldBuildProject = true,
      )
    }
  }

  // TODO: recompile only the targets that are actually needed
  override fun compileFilesAndDependencies(files: Collection<VirtualFile>): Unit = compileProject()
}
