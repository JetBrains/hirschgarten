package org.jetbrains.plugins.bsp.flow.open

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.toNioPath
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.magicmetamodel.impl.BenchmarkFlags.isBenchmark
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFile
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizard.ImportProjectWizard
import org.jetbrains.plugins.bsp.flow.open.wizard.NewConnection
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.server.connection.BspConnection
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspFileConnection
import org.jetbrains.plugins.bsp.server.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.plugins.bsp.utils.RunConfigurationProducersDisabler
import kotlin.system.exitProcess

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isBspProject) {
      doRunActivity(project)
    }
  }

  private suspend fun doRunActivity(project: Project) {
    registerBspToolWindow(project)

    RunConfigurationProducersDisabler(project)

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

    try {
      showWizardAndInitializeConnectionIfApplicable(project)
    } catch (e: Exception) {
      bspSyncConsole.startTask(
        taskId = "bsp-pre-import",
        title = BspPluginBundle.message("console.task.pre.import.title"),
        message = BspPluginBundle.message("console.task.pre.import.in.progress"),
      )
      bspSyncConsole.finishTask("bsp-pre-import",
        BspPluginBundle.message("console.task.pre.import.failed"), FailureResultImpl(e))
    }
  }

  private suspend fun showWizardAndInitializeConnectionIfApplicable(project: Project) {
    val connection = BspConnectionService.getInstance(project).value
    val isBspConnectionKnown = isBspConnectionKnownOrThrow(connection)

    if (project.isNewProject() || !isBspConnectionKnown) {
      val connectionFileOrNewConnection =
        if (isBenchmark()) {
          benchmarkConnection(project)
        } else {
          withContext(Dispatchers.EDT) {
            showWizardAndGetResult(project)
          }
        }

      initializeConnectionOrCloseProject(connectionFileOrNewConnection, project)

      if (connectionFileOrNewConnection != null) {
        SyncProjectTask(project).execute(
          shouldRunInitialSync = true,
          shouldBuildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
          shouldRunResync = BspFeatureFlags.isBuildProjectOnSyncEnabled,
          shouldReloadConnection = false,
        )
      }
    }
  }

  private fun benchmarkConnection(project: Project): ConnectionFile {
    try {
      val connectionFilePath = project.basePath?.toNioPath()?.resolve(".bsp/bazelbsp.json")
      val connectionFile = VfsUtil.findFileByIoFile(connectionFilePath?.toFile()!!, false)!!
      val parsed = LocatedBspConnectionDetailsParser.parseFromFile(connectionFile)
      return ConnectionFile(parsed.bspConnectionDetails!!, parsed.connectionFileLocation)
    } catch (e: Throwable) {
      e.printStackTrace(System.out)
      println("BENCHMARK: Could not create benchmark connection file")
      exitProcess(1)
    }
  }

  private fun isBspConnectionKnownOrThrow(connection: BspConnection?): Boolean =
    if (connection is BspFileConnection) {
      if (connection.locatedConnectionFile.bspConnectionDetails == null)
        error("Parsing connection file '${connection.locatedConnectionFile.connectionFileLocation}' failed!")
      true
    } else {
      connection != null
    }

  private fun showWizardAndGetResult(
    project: Project,
  ): ConnectionFileOrNewConnection? {
    val bspConnectionDetailsGeneratorProvider = BspConnectionDetailsGeneratorProvider(
      project.rootDir,
      BspConnectionDetailsGeneratorExtension.extensions(),
    )
    val wizard = ImportProjectWizard(project, bspConnectionDetailsGeneratorProvider)
    return if (wizard.showAndGet()) wizard.connectionFileOrNewConnectionProperty.get()
    else null
  }

  private fun initializeConnectionOrCloseProject(
    connectionFileOrNewConnection: ConnectionFileOrNewConnection?,
    project: Project,
  ) =
    when (connectionFileOrNewConnection) {
      is NewConnection -> initializeNewConnectionFromGenerator(project, connectionFileOrNewConnection)
      is ConnectionFile -> initializeConnectionFromFile(project, connectionFileOrNewConnection)
      null -> {
        // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
        AppUIExecutor.onUiThread().execute {
          CloseProjectWindowHelper().windowClosing(project)
        }
      }
    }

  private fun initializeNewConnectionFromGenerator(
    project: Project,
    newConnection: NewConnection,
  ) {
    val generator = newConnection.generator
    val bspGeneratorConnection = BspGeneratorConnection(project, generator)

    val bspConnectionService = BspConnectionService.getInstance(project)
    bspConnectionService.value = bspGeneratorConnection
  }

  private fun initializeConnectionFromFile(project: Project, connectionFileInfo: ConnectionFile) {
    val bspFileConnection = BspFileConnection(
      project,
      LocatedBspConnectionDetails(
        connectionFileInfo.bspConnectionDetails,
        connectionFileInfo.connectionFile,
      ),
    )

    val bspConnectionService = BspConnectionService.getInstance(project)
    bspConnectionService.value = bspFileConnection
  }
}
