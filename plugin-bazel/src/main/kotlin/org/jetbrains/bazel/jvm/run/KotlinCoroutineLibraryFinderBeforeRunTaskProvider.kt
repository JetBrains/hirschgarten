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
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.findFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.text.SemVer
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import org.jetbrains.kotlin.idea.debugger.coroutine.DebuggerConnection
import java.nio.file.Path
import kotlin.io.path.pathString

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
        val coroutinesJarAbsolutePath = project.rootDir.findFile(coroutinesJarPath.pathString)?.path ?: return@withBackgroundProgress
        calculateKotlinCoroutineParams(environment, coroutinesJarAbsolutePath)
      }
    }
    return true
  }

  private fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, coroutinesJarPath: String) {
    val parameters = JavaParameters()
    addDebuggerAgent(parameters, environment.project, false)
    parameters.vmParametersList.add("-javaagent:${coroutinesJarPath}")
    val jvmFlags = parameters.vmParametersList
      .parameters
      .map { "--jvmopt=$it" }
    environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY)
      ?.set(jvmFlags)
  }
}

internal fun retrieveKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return emptyList()
  return environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY).get() ?: emptyList()
}

internal fun attachCoroutinesDebuggerConnection(runConfiguration: RunConfigurationBase<*>) {
  DebuggerConnection(
    project = runConfiguration.project,
    configuration = runConfiguration,
    params = JavaParameters(),
    shouldAttachCoroutineAgent = false,
    alwaysShowPanel = true,
  )
}

private val MIN_COROUTINES_VERSION = SemVer.parseFromText("1.3.8")

private suspend fun Project.findClassPathByTargetLabel(label: Label): BspJvmClasspath = connection.runWithServer {
  it.workspaceTargetClasspathQuery(WorkspaceTargetClasspathQueryParams(label))
}

private fun BspJvmClasspath.findLatestCoroutinesJarRootRelativePath(): Path? {
  val (path, version) = findLatestJarByName("kotlinx-coroutines-core-jvm")
    ?: findLatestJarByName("kotlin-coroutines-core")
    ?: return null
  if (version < MIN_COROUTINES_VERSION) return null
  return path
}

private fun BspJvmClasspath.findLatestJarByName(name: String): Pair<Path, SemVer>? {
  return buildSet { addAll(compileClasspath); addAll(runtimeClasspath) }
    .mapNotNull { it.extractPathWithVersionBy(name) }
    .maxByOrNull { (_, version) -> version }
}

private fun Path.extractPathWithVersionBy(name: String): Pair<Path, SemVer>? {
  val lastElement = lastOrNull() ?: return null
  val lastElementString = lastElement.pathString
  if (!lastElementString.contains(name)) return null
  val rawVersion = lastElement.pathString.substringAfterLast("$name-").substringBefore(".jar")
  val version = SemVer.parseFromText(rawVersion) ?: return null
  return this to version
}
