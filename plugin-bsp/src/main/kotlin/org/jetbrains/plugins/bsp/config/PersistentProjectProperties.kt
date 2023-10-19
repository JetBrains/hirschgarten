package org.jetbrains.plugins.bsp.config

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.plugins.bsp.flow.open.BuildToolId

private const val IS_BSP_PROJECT_KEY = "org.jetbrains.bsp.is.bsp.project"

public var Project.isBspProject: Boolean
  get() = properties.getBoolean(IS_BSP_PROJECT_KEY, false)
  set(value) = properties.setValue(IS_BSP_PROJECT_KEY, value)

private const val PROJECT_ROOT_DIR_KEY = "org.jetbrains.bsp.project.root.dir"

public var Project.rootDir: VirtualFile
  get() = properties.getValue(PROJECT_ROOT_DIR_KEY)
    ?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    ?: error("$PROJECT_ROOT_DIR_KEY value not set!")
  set(value) = properties.setValue(PROJECT_ROOT_DIR_KEY, value.url)

private const val BUILD_TOOL_ID_KEY = "org.jetbrains.bsp.build.tool.id"

public var Project.buildToolId: BuildToolId
  get() = properties.getValue(BUILD_TOOL_ID_KEY)
    ?.let { BuildToolId(it) }
    ?: error("$BUILD_TOOL_ID_KEY value not set!")
  set(value) = properties.setValue(BUILD_TOOL_ID_KEY, value.id)

private val Project.properties: PropertiesComponent
  get() = PropertiesComponent.getInstance(this)
