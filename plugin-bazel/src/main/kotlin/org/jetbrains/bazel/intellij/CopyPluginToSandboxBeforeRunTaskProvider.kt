package org.jetbrains.bazel.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.toPath

private val PROVIDER_ID = Key.create<CopyPluginToSandboxBeforeRunTaskProvider.Task>("CopyPluginToSandboxBeforeRunTaskProvider")

public class CopyPluginToSandboxBeforeRunTaskProvider : BeforeRunTaskProvider<CopyPluginToSandboxBeforeRunTaskProvider.Task>() {
  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BazelPluginBundle.message("console.task.copy.plugin.to.sandbox")

  override fun createTask(configuration: RunConfiguration): Task? =
    if (configuration is BazelRunConfiguration) {
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
    val runConfiguration = environment.runProfile as? BazelRunConfiguration ?: return false
    if (runConfiguration.handler !is IntellijPluginRunHandler) return false
    val pluginSandbox =
      checkNotNull(environment.getUserData(INTELLIJ_PLUGIN_SANDBOX_KEY)) {
        "INTELLIJ_PLUGIN_SANDBOX_KEY must be passed"
      }

    val pluginJars = mutableListOf<Path>()

    val targetUtils = configuration.project.targetUtils
    for (target in runConfiguration.targets) {
      val targetInfo = targetUtils.getBuildTargetForLabel(target)
      val module = targetInfo?.getModule(environment.project) ?: continue
      OrderEnumerator.orderEntries(module).librariesOnly().recursively().withoutSdk().forEachLibrary { library ->
        // Use URLs directly because getFiles will be empty until VFS refresh.
        library
          .getUrls(OrderRootType.CLASSES)
          .mapNotNull { "file://" + it.removePrefix("jar://").removeSuffix("!/") }
          .map { URI.create(it).toPath() }
          .forEach { pluginJars.add(it) }
        true
      }
    }

    if (pluginJars.isEmpty()) {
      showError(BazelPluginBundle.message("console.task.exception.no.plugin.jars"))
      return false
    }
    for (pluginJar in pluginJars) {
      if (!pluginJar.exists()) {
        showError(BazelPluginBundle.message("console.task.exception.plugin.jar.not.found", pluginJar))
        return false
      }
      try {
        pluginSandbox.createDirectories()
        pluginJar.copyTo(pluginSandbox.resolve(pluginJar.name), overwrite = true)
      } catch (e: IOException) {
        val errorMessage =
          BazelPluginBundle.message(
            "console.task.exception.plugin.jar.could.not.copy",
            pluginJar,
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
    val title = BazelPluginBundle.message("console.task.exception.copy.plugin.to.sandbox")
    BazelBalloonNotifier.warn(title, message)
  }
}
