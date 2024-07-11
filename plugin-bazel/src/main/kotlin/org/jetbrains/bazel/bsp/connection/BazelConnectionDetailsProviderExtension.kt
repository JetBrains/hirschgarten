package org.jetbrains.bazel.bsp.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BspPluginTemplates
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.coroutines.CoroutineService
import org.jetbrains.bazel.settings.bazelApplicationSettings
import org.jetbrains.bazel.settings.serverDetectedJdk
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.install.BspConnectionDetailsCreator
import org.jetbrains.bsp.bazel.install.EnvironmentCreator
import org.jetbrains.bsp.bazel.installationcontext.InstallationContext
import org.jetbrains.bsp.bazel.installationcontext.InstallationContextJavaPathEntity
import org.jetbrains.bsp.bazel.installationcontext.InstallationContextJavaPathEntityMapper
import org.jetbrains.bsp.protocol.utils.parseBspConnectionDetails
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

private class DotBazelBspCreator(projectPath: VirtualFile) : EnvironmentCreator(projectPath.toNioPath()) {
  override fun create() {
    createDotBazelBsp()
  }
}

private const val DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject"
private const val BAZEL_BSP_CONNECTION_FILE_RELATIVE_PATH = ".bsp/bazelbsp.json"

private const val BAZEL_BSP_CONNECTION_FILE_ARGV_JAVA_INDEX = 0

internal class BazelConnectionDetailsProviderExtension: ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean {
    project.stateService.projectPath = projectPath
    project.stateService.connectionFile = projectPath.findFile(BAZEL_BSP_CONNECTION_FILE_RELATIVE_PATH)

    if (project.stateService.connectionFile == null) {
      initializeProjectViewFile(projectPath)
    }

    return true
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
  ): BspConnectionDetails? {
    val connectionFile = project.stateService.connectionFile

    return if (connectionFile != null) {
      connectionFile.doProvideNewConnectionDetails(currentConnectionDetails)
    } else {
      doProvideNewConnectionDetails(project, currentConnectionDetails)
    }
  }

  private fun VirtualFile.doProvideNewConnectionDetails(currentConnectionDetails: BspConnectionDetails?): BspConnectionDetails? =
    parseBspConnectionDetails()?.takeIf { it != currentConnectionDetails }

  private fun doProvideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?
  ): BspConnectionDetails? {
    val javaBin = calculateSelectedJavaBin()
    val customJvmOptions = bazelApplicationSettings.serverSettings.customJvmOptions

    return if (currentConnectionDetails?.hasNotChanged(javaBin, customJvmOptions) == true) {
      null
    } else {
      calculateNewConnectionDetails(project, javaBin, customJvmOptions)
    }
  }

  private fun calculateSelectedJavaBin(): Path {
    val selectedJdk = bazelApplicationSettings.serverSettings.selectedJdk
    return if (selectedJdk.name == serverDetectedJdk.name) InstallationContextJavaPathEntityMapper.default().value
    else selectedJdk.toJavaBin()
  }

  private fun Sdk.toJavaBin(): Path =
    homePath?.let { Path(it) }?.resolve("bin")?.resolve("java") ?: error("Cannot obtain jdk home path for $name")

  private fun BspConnectionDetails.hasNotChanged(javaBin: Path, customJvmOptions: List<String>): Boolean =
    version == Constants.VERSION
        && argv[BAZEL_BSP_CONNECTION_FILE_ARGV_JAVA_INDEX] == javaBin.toAbsolutePath().toString()
        && getOptions() == customJvmOptions

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-972
  private fun BspConnectionDetails.getOptions(): List<String> =
    argv.drop(BAZEL_BSP_CONNECTION_FILE_ARGV_CLASSPATH_INDEX + 1) // all values before added jvm options
      .dropLast(4) // remove mind class and options

  private fun calculateNewConnectionDetails(project: Project, javaBin: Path, customJvmOptions: List<String>): BspConnectionDetails {
    val projectPath = project.stateService.projectPath ?: error("Cannot obtain project path. Please reopen the project")
    val installationContext = InstallationContext(
      javaPath = InstallationContextJavaPathEntity(javaBin),
      debuggerAddress = null,
      bazelWorkspaceRootDir = projectPath.toNioPath(),
      projectViewFilePath =  projectPath.toNioPath().toAbsolutePath().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME),
    )
    val dotBazelBspCreator = DotBazelBspCreator(projectPath)

    CoroutineService.getInstance(project).start {
      withContext(Dispatchers.EDT) {
        dotBazelBspCreator.create()
      }
    }

    return BspConnectionDetailsCreator(installationContext, false).create()
      .apply { updateClasspath() }
      .apply { addCustomJvmOptions(customJvmOptions) }
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-972
  private fun BspConnectionDetails.addCustomJvmOptions(customJvmOptions: List<String>) {
    argv.addAll(BAZEL_BSP_CONNECTION_FILE_ARGV_CLASSPATH_INDEX + 1, customJvmOptions)
  }
}

internal data class BazelConnectionDetailsProviderExtensionState(
  var projectPath: String? = null,
  var connectionFile: String? = null
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
  var connectionFile: VirtualFile? = null

  override fun getState(): BazelConnectionDetailsProviderExtensionState? =
    BazelConnectionDetailsProviderExtensionState(
      projectPath = projectPath?.url,
      connectionFile = connectionFile?.url,
    )

  override fun loadState(state: BazelConnectionDetailsProviderExtensionState) {
    val virtualFileManager = VirtualFileManager.getInstance()

    projectPath = state.projectPath?.let { virtualFileManager.findFileByUrl(it) }
    connectionFile = state.connectionFile?.let { virtualFileManager.findFileByUrl(it) }
  }
}

private val Project.stateService: BazelConnectionDetailsProviderExtensionService
  get() = getService(BazelConnectionDetailsProviderExtensionService::class.java)
