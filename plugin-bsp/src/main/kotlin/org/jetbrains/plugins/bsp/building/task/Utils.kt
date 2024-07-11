package org.jetbrains.plugins.bsp.building.task

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.impl.ProjectTaskList

internal fun createAllBspOnlyModuleBuildTasks(project: Project): ProjectTask =
  createAllCustomModuleBuildTasks(project, ::BspOnlyModuleBuildTask)

internal fun createAllJpsOnlyModuleBuildTasks(project: Project): ProjectTask =
  createAllCustomModuleBuildTasks(project, ::JpsOnlyModuleBuildTask)

private fun <T : CustomModuleBuildTask> createAllCustomModuleBuildTasks(
  project: Project,
  factory: (Module) -> T,
): ProjectTask =
  ModuleManager.getInstance(project).modules.let { modules -> ProjectTaskList(modules.map { factory(it) }) }
