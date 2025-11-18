package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.engine.AsyncStacksUtils.addDebuggerAgent
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Key
import com.intellij.util.text.SemVer
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.getModule
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
    val module = target.getModule(project) ?: return true
    val coroutinesJarPath = module.findLatestCoroutinesJar() ?: return true
    calculateKotlinCoroutineParams(environment, coroutinesJarPath)
    return true
  }

  private fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, coroutinesJarPath: String) {
    val parameters = JavaParameters()
    addDebuggerAgent(parameters, environment.project, false)
    parameters.vmParametersList.add("-javaagent:${coroutinesJarPath}")
    val jvmFlags = parameters.vmParametersList
      .parameters
      .map { "--jvmopt=$it" }
    environment
      .getCopyableUserData(COROUTINE_JVM_FLAGS_KEY)
      ?.set(jvmFlags)
  }
}

internal fun retrieveKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return emptyList()
  return environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY).get() ?: emptyList()
}

private val MIN_COROUTINES_VERSION = SemVer.parseFromText("1.3.8")

private fun Module.findLatestCoroutinesJar(): String? {
  val (path, version) = findLatestJarRecursively("kotlinx-coroutines-core-jvm")
    ?: findLatestJarRecursively("kotlin-coroutines-core")
    ?: return null
  if (version < MIN_COROUTINES_VERSION) return null
  return path
}

private fun Module.findLatestJarRecursively(name: String) = ModuleRootManager
  .getInstance(this)
  .orderEntries()
  .recursively()
  .classes()
  .pathsList
  .pathList
  .mapNotNull { it.extractPathWithVersionBy(name) }
  .maxByOrNull { (_, version) -> version }

private fun String.extractPathWithVersionBy(name: String): Pair<String, SemVer>? {
  if (!this.contains(name)) return null
  val rawVersion = this.substringAfterLast("$name-").substringBefore(".jar")
  val version = SemVer.parseFromText(rawVersion) ?: return null
  return this to version
}
