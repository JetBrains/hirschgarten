package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

data class BspProjectPropertiesState(
  var isBspProject: Boolean = false,
  var isInitialized: Boolean = false,
  var rootDirUrl: String? = null,
  var buildToolId: String? = null,
  var defaultJdkName: String? = null,
  var openedTimesSinceLastStartupResync: Int = 0,
)

@Service(Service.Level.PROJECT)
@State(
  name = "BspProjectProperties",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class BspProjectProperties : PersistentStateComponent<BspProjectPropertiesState> {
  var isBspProject: Boolean = false
  var rootDir: VirtualFile? = null
  var buildToolId: BuildToolId = bspBuildToolId
  var defaultJdkName: String? = null

  /**
   * if the opened times since the last startup resync is equal to 1,
   * it indicates it is the first successful startup project open.
   */
  var openedTimesSinceLastStartupResync: Int = 0

  override fun getState(): BspProjectPropertiesState? =
    BspProjectPropertiesState(
      isBspProject = isBspProject,
      rootDirUrl = rootDir?.url,
      buildToolId = buildToolId.id,
      defaultJdkName = defaultJdkName,
      openedTimesSinceLastStartupResync = openedTimesSinceLastStartupResync,
    )

  override fun loadState(state: BspProjectPropertiesState) {
    isBspProject = state.isBspProject
    rootDir = state.rootDirUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    defaultJdkName = state.defaultJdkName
    buildToolId = state.buildToolId?.let { BuildToolId(it) } ?: bspBuildToolId
    openedTimesSinceLastStartupResync = state.openedTimesSinceLastStartupResync
  }
}

val Project.bspProjectProperties: BspProjectProperties
  get() = service<BspProjectProperties>()

var Project.isBspProject: Boolean
  get() = bspProjectProperties.isBspProject
  set(value) {
    bspProjectProperties.isBspProject = value
  }

var Project.openedTimesSinceLastStartupResync: Int
  get() = bspProjectProperties.openedTimesSinceLastStartupResync
  set(value) {
    bspProjectProperties.openedTimesSinceLastStartupResync = value
  }

var Project.rootDir: VirtualFile
  get() =
    bspProjectProperties.rootDir
      ?: error("BSP project root dir is not set. Reimport the project to fix this.")
  set(value) {
    bspProjectProperties.rootDir = value
  }

var Project.buildToolId: BuildToolId
  get() =
    bspProjectProperties.buildToolId
  set(value) {
    bspProjectProperties.buildToolId = value
  }

var Project.defaultJdkName: String?
  get() =
    bspProjectProperties.defaultJdkName
  set(value) {
    bspProjectProperties.defaultJdkName = value
  }
