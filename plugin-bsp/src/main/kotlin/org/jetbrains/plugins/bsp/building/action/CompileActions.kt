package org.jetbrains.plugins.bsp.building.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskManager
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.plugins.bsp.building.task.createAllBspOnlyModuleBuildTasks
import org.jetbrains.plugins.bsp.building.task.createAllJpsOnlyModuleBuildTasks
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

internal abstract class CustomCompileProjectAction(text: String) : SuspendableAction(text) {
  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isVisible = project.isBspProject
    e.presentation.isEnabled = !project.isBuildInProgress()
  }

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val task = getProjectTask(project)
    val projectTaskManager = ProjectTaskManager.getInstance(project)
    projectTaskManager.run(task)
  }

  public abstract fun getProjectTask(project: Project): ProjectTask
}

internal fun Project.isBuildInProgress() =
  CompilerManager.getInstance(this).isCompilationActive ||
    BspConsoleService.getInstance(this).bspBuildConsole.hasTasksInProgress()

internal class CompileProjectWithBspAction :
  CustomCompileProjectAction(BspPluginBundle.message("project.task.action.name.build.project.bsp")) {
  override fun getProjectTask(project: Project): ProjectTask = createAllBspOnlyModuleBuildTasks(project)
}

internal class CompileProjectWithJpsAction :
  CustomCompileProjectAction(BspPluginBundle.message("project.task.action.name.build.project.jps")) {
  override fun getProjectTask(project: Project): ProjectTask = createAllJpsOnlyModuleBuildTasks(project)

  override fun update(project: Project, e: AnActionEvent) {
    super.update(project, e)
    e.presentation.isEnabled = JpsFeatureFlags.isJpsCompilationEnabled && e.presentation.isEnabled
  }
}
