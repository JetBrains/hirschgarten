package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.engine.AsyncStacksUtils.addDebuggerAgent
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.PathsList
import com.intellij.util.text.SemVer
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.sync.includesKotlin
import org.jetbrains.bazel.target.ModuleTargetService
import org.jetbrains.bazel.target.targetUtils

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
    val runConfiguration = BazelRunConfiguration.get(environment)
    // skipping this task for non-debugging run config
    if (environment.executor !is DefaultDebugExecutor) return true
    val project = environment.project
    val target = runConfiguration.targets.single()
    val targetInfo = project.targetUtils.getBuildTargetForLabel(target) ?: return true
    if (!targetInfo.kind.includesKotlin() || !targetInfo.kind.isExecutable) return true
    runBlockingMaybeCancellable {
      withBackgroundProgress(project, BazelPluginBundle.message("background.task.description.preparing.for.debugging.kotlin", target)) {
        val result = project.findClassPathByTargetLabel(targetInfo.id)
        val coroutinesJarPath = result.findLatestCoroutinesJarRootRelativePath() ?: return@withBackgroundProgress
        calculateKotlinCoroutineParams(environment, coroutinesJarPath.path)
      }
    }
    return true
  }

  private fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, coroutinesJarPath: String) {
    val parameters = JavaParameters()
    addDebuggerAgent(parameters, environment.project, false)
    parameters.vmParametersList.add("-javaagent:${coroutinesJarPath}")
    environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY)
      ?.set(parameters.vmParametersList.parameters)
  }
}

internal fun retrieveKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return emptyList()
  return environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY).get() ?: emptyList()
}

internal fun attachCoroutinesDebuggerConnection(runConfiguration: RunConfigurationBase<*>) {
  KotlinCoroutinesHelper.ep.extensionList.forEach { it.attachCoroutinesDebuggerConnection(runConfiguration.project, runConfiguration) }
}

private val MIN_COROUTINES_VERSION = SemVer.parseFromText("1.3.8")

private fun Project.findClassPathByTargetLabel(label: Label): List<VirtualFile> =
  service<ModuleTargetService>()
    .findLegacyModulesByLabel(label = label)
    .flatMap { module ->
      val jars = PathsList()
      OrderEnumerator
        .orderEntries(module)
        .recursively()
        .withoutSdk()
        .roots(OrderRootType.CLASSES)
        .collectPaths(jars)
      jars.virtualFiles
    }

private fun List<VirtualFile>.findLatestCoroutinesJarRootRelativePath(): VirtualFile? {
  val (path, version) = findLatestJarByName("kotlinx-coroutines-core-jvm")
                        ?: findLatestJarByName("kotlin-coroutines-core")
                        ?: return null
  if (version < MIN_COROUTINES_VERSION) return null
  return path
}

private fun List<VirtualFile>.findLatestJarByName(name: String): Pair<VirtualFile, SemVer>? {
  return this
    .mapNotNull { it.extractPathWithVersionBy(name) }
    .maxByOrNull { (_, version) -> version }
}

private fun VirtualFile.extractPathWithVersionBy(expectedName: String): Pair<VirtualFile, SemVer>? {
  val name = this.name
  if (!name.contains(expectedName)) return null
  val rawVersion = name.substringAfterLast("$expectedName-").substringBefore(".jar")
  val version = SemVer.parseFromText(rawVersion) ?: return null
  return this to version
}
