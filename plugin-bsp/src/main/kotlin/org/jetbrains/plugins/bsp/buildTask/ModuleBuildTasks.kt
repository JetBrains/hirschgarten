package org.jetbrains.plugins.bsp.buildTask

import com.intellij.openapi.module.Module
import com.intellij.task.ModuleBuildTask
import org.jetbrains.plugins.bsp.config.BspPluginBundle

abstract class CustomModuleBuildTask(private val module: Module) : ModuleBuildTask {
  override fun isIncrementalBuild(): Boolean = true

  override fun getModule(): Module = module

  override fun isIncludeDependentModules(): Boolean = true

  override fun isIncludeRuntimeDependencies(): Boolean = false
}

class BspOnlyModuleBuildTask(module: Module) : CustomModuleBuildTask(module) {
  override fun getPresentableName(): String = BspPluginBundle.message("project.task.name.module.0.build.task.bsp", module.name)
}

class JpsOnlyModuleBuildTask(module: Module) : CustomModuleBuildTask(module) {
  override fun getPresentableName(): String = BspPluginBundle.message("project.task.name.module.0.build.task.jps", module.name)
}
