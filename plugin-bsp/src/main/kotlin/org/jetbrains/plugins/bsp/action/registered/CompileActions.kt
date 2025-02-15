package org.jetbrains.plugins.bsp.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskManager
import com.intellij.task.impl.ProjectTaskList
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.buildTask.BspOnlyModuleBuildTask
import org.jetbrains.plugins.bsp.buildTask.CustomModuleBuildTask
import org.jetbrains.plugins.bsp.buildTask.JpsOnlyModuleBuildTask
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.ui.console.isBuildInProgress

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

  abstract fun getProjectTask(project: Project): ProjectTask
}

internal class CompileProjectWithBspAction :
  CustomCompileProjectAction(BspPluginBundle.message("project.task.action.name.build.project.bsp")) {
  override fun getProjectTask(project: Project): ProjectTask = createAllCustomModuleBuildTasks(project, ::BspOnlyModuleBuildTask)
}

internal class CompileProjectWithJpsAction :
  CustomCompileProjectAction(BspPluginBundle.message("project.task.action.name.build.project.jps")) {
  override fun getProjectTask(project: Project): ProjectTask = createAllCustomModuleBuildTasks(project, ::JpsOnlyModuleBuildTask)

  override fun update(project: Project, e: AnActionEvent) {
    super.update(project, e)
    e.presentation.isEnabled = JpsFeatureFlags.isJpsCompilationEnabled && e.presentation.isEnabled
  }
}

private fun <T : CustomModuleBuildTask> createAllCustomModuleBuildTasks(project: Project, factory: (Module) -> T): ProjectTask =
  ModuleManager.getInstance(project).modules.let { modules -> ProjectTaskList(modules.map { factory(it) }) }
