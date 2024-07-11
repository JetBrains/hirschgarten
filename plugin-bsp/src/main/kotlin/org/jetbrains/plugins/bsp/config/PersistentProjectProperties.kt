package org.jetbrains.plugins.bsp.config

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.plugins.bsp.extension.points.BuildToolId

private const val IS_BSP_PROJECT_KEY = "org.jetbrains.bsp.is.bsp.project"

public var Project.isBspProject: Boolean
  get() = properties.getBoolean(IS_BSP_PROJECT_KEY, false)
  set(value) = properties.setValue(IS_BSP_PROJECT_KEY, value)

private const val PROJECT_ROOT_DIR_KEY = "org.jetbrains.bsp.project.root.dir"

public var Project.rootDir: VirtualFile
  get() = properties.getVirtualFileOrThrow(PROJECT_ROOT_DIR_KEY)
  set(value) = properties.setValue(PROJECT_ROOT_DIR_KEY, value.url)

private const val BUILD_TOOL_ID_KEY = "org.jetbrains.bsp.build.tool.id"

public var Project.buildToolId: BuildToolId
  get() = BuildToolId(properties.getValueOrThrow(BUILD_TOOL_ID_KEY))
  set(value) = properties.setValue(BUILD_TOOL_ID_KEY, value.id)

public val Project.buildToolIdOrNull: BuildToolId?
  get() = properties.getValue(BUILD_TOOL_ID_KEY)?.let { BuildToolId(it) }

private const val IS_PROJECT_INITIALIZED = "org.jetbrains.bsp.is.project.initialized"

public var Project.isBspProjectInitialized: Boolean
  get() = properties.getBoolean(IS_PROJECT_INITIALIZED, false)
  set(value) = properties.setValue(IS_PROJECT_INITIALIZED, value)

private val Project.properties: PropertiesComponent
  get() = PropertiesComponent.getInstance(this)

private fun PropertiesComponent.getVirtualFileOrThrow(key: String): VirtualFile =
  getValueOrThrow(key)
    .let { VirtualFileManager.getInstance().findFileByUrl(it) ?: error("Cannot find file by url (url: $it)") }

private fun PropertiesComponent.getValueOrThrow(key: String): String =
  getValue(key) ?: error("$key value not set")
