package org.jetbrains.bazel.jvm.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Key
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.target.targetUtils

// Minimum coroutines library version (1.3.7-255) parsed to be used for comparison. Coroutines
// debugging is not available in earlier versions of the coroutines library.
private val MIN_LIB_VERSION = listOf(1, 3, 7, 255)
private val LIB_PATTERN = "(kotlinx-coroutines-core(-jvm)?)-(\\d[\\w.\\-]+)?\\.jar".toRegex()
private const val PROVIDER_NAME = "KotlinCoroutineLibraryFinderBeforeRunTaskProvider"

private val PROVIDER_ID = Key.create<KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task>(PROVIDER_NAME)

internal class KotlinCoroutineLibraryFinderBeforeRunTaskProvider :
  BeforeRunTaskProvider<KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task>() {
  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = PROVIDER_NAME

  class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun createTask(runConfiguration: RunConfiguration): Task? {
    if (runConfiguration !is BazelRunConfiguration) return null
    val project = runConfiguration.project
    if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return null
    return Task()
  }

  /**
   * always return true for this task as it is just an add-on and should not block the debugging process
   */
  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val runConfiguration = environment.runProfile as BazelRunConfiguration
    // skipping this task for non-debugging run config
    if (environment.executor !is DefaultDebugExecutor) return true
    val project = environment.project
    val target = runConfiguration.targets.single()
    val targetInfo = project.targetUtils.getBuildTargetForLabel(target) ?: return true
    if (!targetInfo.kind.includesKotlin() || !targetInfo.kind.isExecutable) return true
    val module = target.getModule(project) ?: return true
    runBlocking {
      withBackgroundProgress(project, BazelPluginBundle.message("background.task.description.preparing.for.debugging.kotlin", target)) {
        val kotlinxCoroutinePath =
          OrderEnumerator
            .orderEntries(module)
            .recursively()
            .classes()
            .pathsList.pathList
            .firstOrNull { isKotlinxCoroutinesLib(it) }
            ?: return@withBackgroundProgress
        environment.getCopyableUserData(KOTLIN_COROUTINE_LIB_KEY).set(kotlinxCoroutinePath)
        return@withBackgroundProgress
      }
    }
    return true
  }

  private fun isKotlinxCoroutinesLib(jarPath: String): Boolean {
    val m = LIB_PATTERN.find(jarPath)
    if (m != null && m.groupValues.size >= 3) {
      val version = m.groupValues[3]
      return isValidVersion(version)
    }
    return false
  }

  private fun isValidVersion(libVersion: String): Boolean {
    val versionParts = libVersion.split(regex = "[.-]".toRegex())

    val maxLength: Int = MIN_LIB_VERSION.size.coerceAtLeast(versionParts.size)
    for (i in 0..<maxLength) {
      val versionPart = if (i < versionParts.size) versionParts[i].toInt() else 0
      val minVersionPart: Int = if (i < MIN_LIB_VERSION.size) MIN_LIB_VERSION[i] else 0
      val res = versionPart.compareTo(minVersionPart)
      if (res != 0) {
        return res > 0
      }
    }
    return false
  }
}
