package org.jetbrains.plugins.bsp.actions.building

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.util.ModalityUiUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.server.tasks.BuildTargetTask
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

public class BspProjectTaskRunner : ProjectTaskRunner() {

  override fun canRun(project: Project, projectTask: ProjectTask): Boolean {
    val isBspProject = BspProjectPropertiesService.getInstance(project).value.isBspProject
    return isBspProject && canRun(projectTask)
  }

  override fun canRun(projectTask: ProjectTask): Boolean = projectTask is ModuleBuildTask

  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask
  ): Promise<Result> {
    val result = AsyncPromise<Result>()

    ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState(), project.disposed) {
      val res = runModuleBuildTasks(project, tasks.filterIsInstance<ModuleBuildTask>())
      result.setResult(res)
    }

    return result
  }

  private fun runModuleBuildTasks(
    project: Project,
    tasks: List<ModuleBuildTask>
  ): Result {
    val targetsToBuild = obtainTargetsToBuild(project, tasks)
    return buildBspTargets(project, targetsToBuild)
  }

  private fun obtainTargetsToBuild(project: Project, tasks: List<ModuleBuildTask>): List<BuildTarget> {
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

  private fun BuildTarget.belongsToModules(project: Project, moduleNames: List<String>): Boolean {
    val moduleNameProvider =
      MagicMetaModelService.getInstance(project).obtainModuleNameProvider() ?: DefaultModuleNameProvider
    val targetModuleName = moduleNameProvider(this.id)
    return moduleNames.any {
      it == targetModuleName || targetModuleName isSubmoduleOf it
    }
  }

  private infix fun String.isSubmoduleOf(module: String): Boolean =
    this.startsWith("$module.", false)

  private fun buildBspTargets(project: Project, targetsToBuild: List<BuildTarget>): Result {
    val targetIdentifiers = targetsToBuild.filter { it.capabilities.canCompile }.map { it.id }
    val result = BuildTargetTask(project).executeIfConnected(targetIdentifiers)
    return result?.toTaskRunnerResult() ?: TaskRunnerResults.FAILURE
  }

  private fun CompileResult.toTaskRunnerResult() =
    when (statusCode) {
      StatusCode.OK -> TaskRunnerResults.SUCCESS
      StatusCode.CANCELLED -> TaskRunnerResults.ABORTED
      else -> TaskRunnerResults.FAILURE
    }
}
