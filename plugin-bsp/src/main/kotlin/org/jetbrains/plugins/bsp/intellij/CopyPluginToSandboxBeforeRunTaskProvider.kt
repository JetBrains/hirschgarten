package org.jetbrains.plugins.bsp.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.getModule
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.toPath

private val PROVIDER_ID =
  Key.create<CopyPluginToSandboxBeforeRunTaskProvider.Task>("CopyPluginToSandboxBeforeRunTaskProvider")

public class CopyPluginToSandboxBeforeRunTaskProvider :
  BeforeRunTaskProvider<CopyPluginToSandboxBeforeRunTaskProvider.Task>() {
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
    if (runConfiguration.runHandler !is IntellijPluginRunHandler) return false
    val pluginSandbox = checkNotNull(environment.getUserData(INTELLIJ_PLUGIN_SANDBOX_KEY)) {
      "INTELLIJ_PLUGIN_SANDBOX_KEY must be passed"
    }

    val pluginJars = mutableListOf<Path>()

    for (target in runConfiguration.targets) {
      val module = target.getModule(environment.project) ?: continue
      OrderEnumerator.orderEntries(module).librariesOnly().withoutSdk().forEachLibrary { library ->
        // Use URLs directly because getFiles will be empty until everything is indexed.
        library.getUrls(OrderRootType.CLASSES)
          .mapNotNull { "file://" + it.removePrefix("jar://").removeSuffix("!/") }
          .map { URI.create(it).toPath() }
          .forEach { pluginJars.add(it) }
        true
      }
    }

    if (pluginJars.isEmpty()) {
      showError(BspPluginBundle.message("console.task.exception.no.plugin.jars"), environment)
      return false
    }
    for (pluginJar in pluginJars) {
      if (!pluginJar.exists()) {
        showError(BspPluginBundle.message("console.task.exception.plugin.jar.not.found", pluginJar), environment)
        return false
      }
      try {
        pluginSandbox.createDirectories()
        pluginJar.copyTo(pluginSandbox.resolve(pluginJar.name), overwrite = true)
      } catch (e: IOException) {
        val errorMessage = BspPluginBundle.message(
          "console.task.exception.plugin.jar.could.not.copy",
          pluginJar,
          pluginSandbox,
          e.message.orEmpty(),
        )
        showError(errorMessage, environment)
        return false
      }
    }

    return true
  }

  // Throwing an ExecutionException doesn't work from before run tasks, so we have to show the notification ourselves.
  private fun showError(message: String, environment: ExecutionEnvironment) {
    val title = BspPluginBundle.message("console.task.exception.copy.plugin.to.sandbox")
    getNotificationGroup()
      .createNotification(title, message, NotificationType.ERROR)
      .notify(environment.project)
  }

  private fun getNotificationGroup(): NotificationGroup {
    return NotificationGroupManager.getInstance().getNotificationGroup("CopyPluginToSandboxBeforeRunTaskProvider")
  }
}
