package org.jetbrains.plugins.bsp.intellij

import com.google.devtools.intellij.plugin.IntellijPluginDeployTargetInfo
import com.google.devtools.intellij.plugin.IntellijPluginDeployTargetInfo.IntellijPluginDeployFile
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.getModule
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier
import com.google.devtools.intellij.plugin.IntellijPluginDeployTargetInfo.IntellijPluginDeployInfo
import com.google.protobuf.TextFormat
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.toPath

private val PROVIDER_ID = Key.create<CopyPluginToSandboxBeforeRunTaskProvider.Task>("CopyPluginToSandboxBeforeRunTaskProvider")
private val DEPLOY_INFO_FILE_EXTENSION = ".intellij-plugin-debug-target-deploy-info"

public class CopyPluginToSandboxBeforeRunTaskProvider : BeforeRunTaskProvider<CopyPluginToSandboxBeforeRunTaskProvider.Task>() {
  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BspPluginBundle.message("console.task.copy.plugin.to.sandbox")

  override fun createTask(configuration: RunConfiguration): Task? =
    if (configuration is BspRunConfiguration) {
      Task()
    } else {
      null
    }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val runConfiguration = environment.runProfile as? BspRunConfiguration ?: return false
    if (runConfiguration.handler !is IntellijPluginRunHandler) return false
    val pluginSandbox =
      checkNotNull(environment.getUserData(INTELLIJ_PLUGIN_SANDBOX_KEY)) {
        "INTELLIJ_PLUGIN_SANDBOX_KEY must be passed"
      }

    val deployFiles = mutableMapOf<Path, Path>()

    //
    val target = runConfiguration.targets.single()
      // convert uri to a project-local path pointing to the uri + DEPLOY_INFO_FILE_EXTENSION
      val path = Path.of(target.uri)
      val jarPath = path.resolveSibling(path.fileName.toString() + DEPLOY_INFO_FILE_EXTENSION)
      if (path.exists()) {
        val info = Files.newInputStream(jarPath).use { inputStream ->
          val builder = IntellijPluginDeployInfo.newBuilder()
          val parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build()
          parser.merge(InputStreamReader(inputStream, UTF_8), builder)
          builder.build()
        }
        for (file in info.deployFilesList) {
          deployFiles.put(Path.of(file.executionPath), Path.of(file.deployLocation))
      }
    }

    if (deployFiles.isEmpty()) {
      showError(BspPluginBundle.message("console.task.exception.no.plugin.jars"))
      return false
    }
    for ((deployFile, target) in deployFiles) {
      if (!deployFile.exists()) {
        showError(BspPluginBundle.message("console.task.exception.plugin.jar.not.found", deployFile))
        return false
      }
      try {
        pluginSandbox.createDirectories()
        deployFile.copyTo(pluginSandbox.resolve(target), overwrite = true)
      } catch (e: IOException) {
        val errorMessage =
          BspPluginBundle.message(
            "console.task.exception.plugin.jar.could.not.copy",
            deployFile,
            pluginSandbox,
            e.message.orEmpty(),
          )
        showError(errorMessage)
        return false
      }
    }

    return true
  }

  // Throwing an ExecutionException doesn't work from before run tasks, so we have to show the notification ourselves.
  private fun showError(message: String) {
    val title = BspPluginBundle.message("console.task.exception.copy.plugin.to.sandbox")
    BspBalloonNotifier.warn(title, message)
  }
}
