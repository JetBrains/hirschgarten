package org.jetbrains.plugins.bsp.actions.building

import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.toPromise
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifiers
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault

public class BspProjectTaskRunner : ProjectTaskRunner() {
  private val log = logger<BspProjectTaskRunner>()

  override fun canRun(project: Project, projectTask: ProjectTask): Boolean =
    project.isBspProject && canRun(projectTask)

  override fun canRun(projectTask: ProjectTask): Boolean = projectTask is ModuleBuildTask

  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ): Promise<Result> {
    val result = AsyncPromise<Result>()

    val res = runModuleBuildTasks(project, tasks.filterIsInstance<ModuleBuildTask>())
    res.then { result.setResult(it) }

    return result
  }

  private fun runModuleBuildTasks(
    project: Project,
    tasks: List<ModuleBuildTask>,
  ): Promise<Result> {
    val targetsToBuild = obtainTargetsToBuild(project, tasks)
    return buildBspTargets(project, targetsToBuild)
  }

  private fun obtainTargetsToBuild(project: Project, tasks: List<ModuleBuildTask>): List<BuildTargetInfo> {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    return when {
      tasks.isEmpty() -> emptyList()
      tasks.size >= magicMetaModel.getAllLoadedTargets().size ->
        magicMetaModel.getAllLoadedTargets() + magicMetaModel.getAllNotLoadedTargets()

      else -> {
        val moduleNames = tasks.map { it.module.name }
        magicMetaModel.getAllLoadedTargets()
          .filter { it.belongsToModules(project, moduleNames) }
      }
    }
  }

  private fun BuildTargetInfo.belongsToModules(project: Project, moduleNames: List<String>): Boolean {
    val moduleNameProvider = project.findModuleNameProvider().orDefault()
    val targetModuleName = moduleNameProvider(this.id)
    return moduleNames.any {
      it == targetModuleName || targetModuleName isSubmoduleOf it
    }
  }

  private infix fun String.isSubmoduleOf(module: String): Boolean =
    this.startsWith("$module.", false)

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun buildBspTargets(project: Project, targetsToBuild: List<BuildTargetInfo>): Promise<Result> {
    val targetIdentifiers = targetsToBuild.filter { it.capabilities.canCompile }.map { it.id }
    val result = BspCoroutineService.getInstance(project).startAsync {
      runBuildTargetTask(targetIdentifiers.toBsp4JTargetIdentifiers(), project, log)
    }
    return result
      .toPromise()
      .then { it?.toTaskRunnerResult() ?: TaskRunnerResults.FAILURE }
  }

  private fun CompileResult.toTaskRunnerResult() =
    when (statusCode) {
      StatusCode.OK -> TaskRunnerResults.SUCCESS
      StatusCode.CANCELLED -> TaskRunnerResults.ABORTED
      else -> TaskRunnerResults.FAILURE
    }
}
