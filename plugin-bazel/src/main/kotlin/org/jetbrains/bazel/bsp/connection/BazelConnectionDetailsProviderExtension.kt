package org.jetbrains.bazel.bsp.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import coursier.core.Dependency
import coursier.core.Module
import org.jetbrains.bazel.assets.BspPluginTemplates
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.utils.parseBspConnectionDetails
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import scala.collection.immutable.Map
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.javaapi.CollectionConverters
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.writeText

private const val DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject"

internal class BazelConnectionDetailsProviderExtension: ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean {
    project.stateService.projectPath = projectPath

    initializeProjectViewFile(projectPath)
    writeAction { generateConnectionFile(project, projectPath) }

    return project.connectionFile != null
  }

  private fun initializeProjectViewFile(projectPath: VirtualFile) {
    val projectViewFilePath = calculateProjectViewFilePath(projectPath)
    setDefaultProjectViewFilePathContentIfNotExists(projectViewFilePath)
  }

  private fun calculateProjectViewFilePath(projectPath: VirtualFile): Path =
    projectPath.toNioPath().toAbsolutePath().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)

  private fun setDefaultProjectViewFilePathContentIfNotExists(projectViewFilePath: Path) {
    if (!projectViewFilePath.exists()) {
      projectViewFilePath.writeText(BspPluginTemplates.defaultBazelProjectViewContent)
    }
  }

  override fun provideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?
  ): BspConnectionDetails? =
    if (currentConnectionDetails?.version != Constants.VERSION) {
      ApplicationManager.getApplication().invokeAndWait {
        runWriteAction { project.connectionFile?.delete(this) }
      }
      val projectPath = project.stateService.projectPath
        ?: error("Cannot obtain project path, please reimport the project.")

      ApplicationManager.getApplication().invokeAndWait {
        runWriteAction { generateConnectionFile(project, projectPath) }
      }
      project.connectionFile?.parseBspConnectionDetails()
    } else {
      null
    }

  private fun generateConnectionFile(project: Project, projectPath: VirtualFile) {
    executeAndWait(
      command = calculateInstallerCommand(),
      projectPath = projectPath,
      project = project,
    )
  }

  private fun executeAndWait(
    command: List<String>,
    projectPath: VirtualFile,
    project: Project,
  ) {
    val commandStr = command.joinToString(" ")
    val builder = ProcessBuilder(command)
      .directory(projectPath.toNioPath().toFile())
      .withRealEnvs()
      .redirectError(ProcessBuilder.Redirect.PIPE)

    val consoleProcess = builder.start()
    consoleProcess.waitFor()
    if (consoleProcess.exitValue() != 0) {
      error("An error has occurred while running the command: $commandStr")
    }
  }

  private fun calculateInstallerCommand(): List<String> =
    listOf(
      calculateJavaExecPath(),
      "-cp",
      calculateNeededJars(
        org = "org.jetbrains.bsp",
        name = "bazel-bsp",
        version = "3.1.0-20231216-ec66172-NIGHTLY",
      )
        .joinToString(":"),
      "org.jetbrains.bsp.bazel.install.Install",
    )

  private fun calculateJavaExecPath(): String {
    val javaHome = System.getProperty("java.home")
    if (javaHome == null) {
      error("Java needs to be set up before running the plugin")
    } else {
      return "${Paths.get(javaHome, "bin", "java")}"
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun calculateNeededJars(org: String, name: String, version: String): List<String> {
    val attributes = Map.from(CollectionConverters.asScala(mapOf<String, String>()))
    val dependencies = listOf<Dependency>(
      Dependency.apply(
        Module.apply(org, name, attributes),
        version,
      ),
    )
    val fetchTask = coursier
      .Fetch
      .apply()
      .addDependencies(CollectionConverters.asScala(dependencies).toSeq())
    val executionContext = fetchTask.cache().ec()
    val future = fetchTask.io().future(executionContext)
    val futureResult = Await.result(future, Duration.Inf())
    return CollectionConverters.asJava(futureResult as scala.collection.immutable.List<File>).map { it.canonicalPath }
  }

  private val Project.connectionFile: VirtualFile?
    get() = getChild(stateService.projectPath, listOf(".bsp", "bazelbsp.json"))

  private fun getChild(root: VirtualFile?, path: List<String>): VirtualFile? {
    val found: VirtualFile? = path.fold(root) { vf: VirtualFile?, child: String ->
      vf?.refresh(false, false)
      vf?.findChild(child)
    }
    found?.refresh(false, false)
    return found
  }
}


internal data class BazelConnectionDetailsProviderExtensionState(
  var projectPath: String? = null,
)

@State(
  name = "BazelConnectionDetailsProviderExtensionService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelConnectionDetailsProviderExtensionService
  : PersistentStateComponent<BazelConnectionDetailsProviderExtensionState> {
  var projectPath: VirtualFile? = null

  override fun getState(): BazelConnectionDetailsProviderExtensionState? =
    BazelConnectionDetailsProviderExtensionState(
      projectPath = projectPath?.url,
    )

  override fun loadState(state: BazelConnectionDetailsProviderExtensionState) {
    val virtualFileManager = VirtualFileManager.getInstance()

    projectPath = state.projectPath?.let { virtualFileManager.findFileByUrl(it) }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelConnectionDetailsProviderExtensionService =
      project.getService(BazelConnectionDetailsProviderExtensionService::class.java)
  }
}

private val Project.stateService: BazelConnectionDetailsProviderExtensionService
  get() = BazelConnectionDetailsProviderExtensionService.getInstance(this)
