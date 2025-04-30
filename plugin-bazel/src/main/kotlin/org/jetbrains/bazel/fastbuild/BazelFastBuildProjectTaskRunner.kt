package org.jetbrains.bazel.fastbuild

import com.intellij.execution.ExecutionManager
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ModuleFilesBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bsp.protocol.FastBuildParams
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.all
import java.nio.file.Files

class BazelFastBuildProjectTaskRunner: ProjectTaskRunner() {
  override fun canRun(projectTask: ProjectTask): Boolean {
    return when (projectTask) {
      is ModuleFilesBuildTask -> !projectTask.module.project.bazelProjectSettings.enableBuildWithJps
      else -> false
    }
  }

  override fun canRun(
    project: Project,
    projectTask: ProjectTask,
    context: ProjectTaskContext?
  ): Boolean {
    return Registry.`is`(FastBuildUtils.fastBuildEnabledKey) &&
      project.isBazelProject &&
      project.isTrusted() &&
      getRunningBazelConfigs(project).isNotEmpty() &&
      canRun(projectTask)
  }

  override fun run(
    project: Project,
    context: ProjectTaskContext,
    vararg tasks: ProjectTask?
  ): Promise<Result> {
    val moduleBuildTasks = tasks.filterIsInstance<ModuleFilesBuildTask>()
    val result = AsyncPromise<Result>()
    BazelCoroutineService.getInstance(project).startAsync {
      runInEdt {
        FileDocumentManager.getInstance().saveAllDocuments()
      }
      val targetUtils = project.service<TargetUtils>()
      val results = ArrayList<Promise<Result>>()

      moduleBuildTasks.flatMap { it.files.toList() }.forEach { file ->
        val tempDir = Files.createTempDirectory(file.name)
        val buildCommand = project.connection.runWithServer {
          it.fastBuildFile(FastBuildParams(targetUtils.getTargetsForFile(file).first(), file.toNioPath(), tempDir))
        } ?: TODO()
        val tempFastBuildDir = getRunningBazelConfigs(project).first().fastBuildPath
        results.add(FastBuildUtils.fastBuildFiles(project, buildCommand, file, tempFastBuildDir))

      }

      results.all().then { result.setResult(it as Result) }
    }
    return result
  }

  private fun getRunningBazelConfigs(project: Project): List<BazelProcessHandler> = ExecutionManager.getInstance(project)
    .getRunningProcesses().mapNotNull { it as? BazelProcessHandler }
}
