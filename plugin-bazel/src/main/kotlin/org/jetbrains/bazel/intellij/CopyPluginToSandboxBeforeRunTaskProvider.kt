package org.jetbrains.bazel.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import com.google.devtools.intellij.plugin.IntellijPluginDeployTargetInfo.IntellijPluginDeployInfo
import com.google.protobuf.TextFormat
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.label.Label
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val DEPLOY_INFO_EXTENSION = ".intellij-plugin-debug-target-deploy-info"

private val PROVIDER_ID = Key.create<CopyPluginToSandboxBeforeRunTaskProvider.Task>("CopyPluginToSandboxBeforeRunTaskProvider")

public class CopyPluginToSandboxBeforeRunTaskProvider : BeforeRunTaskProvider<CopyPluginToSandboxBeforeRunTaskProvider.Task>() {
  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BazelPluginBundle.message("console.task.copy.plugin.to.sandbox")

  override fun createTask(configuration: RunConfiguration): Task? =
    if (configuration is BazelRunConfiguration) {
      Task()
    }
    else {
      null
    }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val configuration = environment.runProfile as? BazelRunConfiguration ?: return false
    if (configuration.handler !is IntellijPluginRunHandler) return false

    val pluginSandbox = checkNotNull(environment.getUserData(INTELLIJ_PLUGIN_SANDBOX_KEY)) {
      "INTELLIJ_PLUGIN_SANDBOX_KEY must be passed"
    }

    val infoFiles = configuration.targets.mapNotNull { guessDeployInfoPath(configuration.project, it) }
    if (infoFiles.isEmpty()) {
      showError("No deploy info file found for the targets")
      return false
    }

    val executionRoot = BazelBinPathService.getInstance(configuration.project).bazelExecPath?.let(Path::of)
    if (executionRoot == null) {
      showError("Cannot determine the execution root")
      return false
    }

    try {
      for (file in infoFiles) {
        copyDeployFiles(parseDeployInfo(file), executionRoot, pluginSandbox)
      }
    } catch (e: IOException) {
      showError("Cannot deploy plugin files: ${e.message}")
      return false
    }

    return true
  }

  /**
   * Tries to guess the path of the deploy info file based on the target location. Ideally the file could just be found in the target's
   * default output group, but the current BEP infrastructure does not support retrieving build artifacts for a target.
   */
  private fun guessDeployInfoPath(project: Project, targetLabel: Label): Path? {
    val bazelBinPath = BazelBinPathService.getInstance(project).bazelBinPath ?: return null
    val workspaceRoot = project.service<BazelProjectProperties>().rootDir?.toNioPath() ?: return null

    val targetInfo = project.targetUtils.getBuildTargetForLabel(targetLabel) ?: return null

    // required for save relativize call below, but should always hold
    if (!targetInfo.baseDirectory.startsWith(workspaceRoot)) {
      return null
    }

    val deployInfoFile = Path.of(bazelBinPath)
      .resolve(workspaceRoot.relativize(targetInfo.baseDirectory))
      .resolve(targetLabel.targetName + DEPLOY_INFO_EXTENSION)

    return if (Files.exists(deployInfoFile)) deployInfoFile else null
  }

  @Throws(IOException::class)
  private fun parseDeployInfo(deployInfoPath: Path): IntellijPluginDeployInfo {
    val builder = IntellijPluginDeployInfo.newBuilder()
    val parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build()

    Files.newBufferedReader(deployInfoPath, StandardCharsets.UTF_8).use { reader ->
      parser.merge(reader, builder)
    }

    return builder.build()
  }

  @Throws(IOException::class)
  private fun copyDeployFiles(deployInfo: IntellijPluginDeployInfo, executionRoot: Path, pluginSandbox: Path) {
    for (deployFile in deployInfo.deployFilesList) {
      val src = executionRoot.resolve(deployFile.executionPath)
      if (!Files.exists(src)) {
        throw IOException("Deploy info file not found: ${deployFile.executionPath}")
      }

      val dst = pluginSandbox.resolve(deployFile.deployLocation)
      if (dst.parent != null) {
        Files.createDirectories(dst.parent)
      }

      Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  // Throwing an ExecutionException doesn't work from before run tasks, so we have to show the notification ourselves.
  private fun showError(message: String) {
    val title = BazelPluginBundle.message("console.task.exception.copy.plugin.to.sandbox")
    BazelBalloonNotifier.warn(title, message)
  }
}
