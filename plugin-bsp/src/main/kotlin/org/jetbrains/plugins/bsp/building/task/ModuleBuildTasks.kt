package org.jetbrains.plugins.bsp.building.task

import com.intellij.openapi.module.Module
import com.intellij.task.ModuleBuildTask
import org.jetbrains.plugins.bsp.config.BspPluginBundle

internal abstract class CustomModuleBuildTask(private val module: Module): ModuleBuildTask {
  override fun isIncrementalBuild(): Boolean = true
  override fun getModule(): Module = module
  override fun isIncludeDependentModules(): Boolean = true
  override fun isIncludeRuntimeDependencies(): Boolean = false
}

internal class BspOnlyModuleBuildTask(module: Module) : CustomModuleBuildTask(module) {
  override fun getPresentableName(): String =
    BspPluginBundle.message("project.task.name.module.0.build.task.bsp", module.name)
}

internal class JpsOnlyModuleBuildTask(module: Module) : CustomModuleBuildTask(module) {
  override fun getPresentableName(): String =
    BspPluginBundle.message("project.task.name.module.0.build.task.jps", module.name)
}
