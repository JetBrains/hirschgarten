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
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl

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
  var isInitialized: Boolean = false
  var isBspProject: Boolean = false
  var rootDir: VirtualFile? = null
  var buildToolId: BuildToolId? = null
  var defaultJdkName: String? = null

  /**
   * if the opened times since the last startup resync is equal to 1,
   * it indicates it is the first successful startup project open.
   */
  var openedTimesSinceLastStartupResync: Int = 0

  override fun getState(): BspProjectPropertiesState? =
    BspProjectPropertiesState(
      isInitialized = isInitialized,
      isBspProject = isBspProject,
      rootDirUrl = rootDir?.url,
      buildToolId = buildToolId?.id,
      defaultJdkName = defaultJdkName,
      openedTimesSinceLastStartupResync = openedTimesSinceLastStartupResync,
    )

  override fun loadState(state: BspProjectPropertiesState) {
    isInitialized = state.isInitialized
    isBspProject = state.isBspProject
    rootDir = state.rootDirUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    buildToolId = state.buildToolId?.let { BuildToolId(it) }
    defaultJdkName = state.defaultJdkName
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

var Project.isBspProjectInitialized: Boolean
  get() = bspProjectProperties.isInitialized
  set(value) {
    bspProjectProperties.isInitialized = value
  }

val Project.isBspProjectLoaded: Boolean
  get() = isBspProjectInitialized && workspaceModelLoadedFromCache

val Project.workspaceModelLoadedFromCache: Boolean
  get() = (workspaceModel as WorkspaceModelImpl).loadedFromCache

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

val Project.bspProjectName: String
  get() = rootDir.name

var Project.buildToolId: BuildToolId
  get() =
    bspProjectProperties.buildToolId
      ?: error("Project's build tool id is not set. Reimport the project to fix this.")
  set(value) {
    bspProjectProperties.buildToolId = value
  }

val Project.buildToolIdOrDefault: BuildToolId
  get() = bspProjectProperties.buildToolId ?: bspBuildToolId

val Project.buildToolIdOrNull: BuildToolId?
  get() = bspProjectProperties.buildToolId

var Project.defaultJdkName: String?
  get() = bspProjectProperties.defaultJdkName
  set(value) {
    bspProjectProperties.defaultJdkName = value
  }
