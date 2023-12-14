package org.jetbrains.plugins.bsp.flow.open

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import org.jetbrains.bsp.utils.parseBspConnectionDetails
import org.jetbrains.magicmetamodel.impl.BenchmarkFlags.isBenchmark
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspWorkspace
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import org.jetbrains.plugins.bsp.server.connection.DefaultBspConnection
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.plugins.bsp.utils.RunConfigurationProducersDisabler
import kotlin.system.exitProcess

private class BspBenchmarkConnectionFileProvider(
  private val bspConnectionDetails: BspConnectionDetails,
) : ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId = BuildToolId("bsp benchmark")

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean = true

  override fun provideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?,
  ): BspConnectionDetails =
    bspConnectionDetails
}

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isBspProject) {
      project.executeForBspProject()
    }
  }

  private suspend fun Project.executeForBspProject() {
    executeEveryTime()

    print("executeForBspProject ${isNewProject()  }")
    if (isNewProject()) {
      executeForNewProject()
    }

    postActivity()
  }

  private suspend fun Project.executeEveryTime() {
    registerBspToolWindow(this)
    RunConfigurationProducersDisabler(this)
  }

  private suspend fun Project.executeForNewProject() {
    try {
      runSync(this)
    } catch (e: Exception) {
      val bspSyncConsole = BspConsoleService.getInstance(this).bspSyncConsole
      bspSyncConsole.startTask(
        taskId = "bsp-pre-import",
        title = BspPluginBundle.message("console.task.pre.import.title"),
        message = BspPluginBundle.message("console.task.pre.import.in.progress"),
      )
      bspSyncConsole.finishTask(
        taskId = "bsp-pre-import",
        message = BspPluginBundle.message("console.task.pre.import.failed"),
        result = FailureResultImpl(e),
      )
    }
  }

  private suspend fun runSync(project: Project) {
    val connectionDetailsProviderExtension =
      if (isBenchmark()) benchmarkConnectionFileProvider(project)
      else ConnectionDetailsProviderExtension.ep.withBuildToolIdOrDefault(project.buildToolId)

    project.connection = DefaultBspConnection(project, connectionDetailsProviderExtension)

    val wasFirstOpeningSuccessful = connectionDetailsProviderExtension.onFirstOpening(project, project.rootDir)

    if (wasFirstOpeningSuccessful) {
      SyncProjectTask(project).execute(
        shouldRunInitialSync = true,
        shouldBuildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
        shouldRunResync = BspFeatureFlags.isBuildProjectOnSyncEnabled,
      )
    } else {
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
      AppUIExecutor.onUiThread().execute {
        CloseProjectWindowHelper().windowClosing(project)
      }
    }
  }

  private fun benchmarkConnectionFileProvider(project: Project): ConnectionDetailsProviderExtension {
    try {
      val connectionFilePath = project.basePath?.toNioPathOrNull()?.resolve(".bsp/bazelbsp.json")
      val connectionFile = VfsUtil.findFileByIoFile(connectionFilePath?.toFile()!!, false)!!
      val connectionDetails = connectionFile.parseBspConnectionDetails()!!
      return BspBenchmarkConnectionFileProvider(connectionDetails)
    } catch (e: Throwable) {
      e.printStackTrace(System.out)
      println("BENCHMARK: Could not create benchmark connection file")
      exitProcess(1)
    }
  }

  private fun Project.postActivity() {
    BspWorkspace.getInstance(this).initialize()
  }
}
