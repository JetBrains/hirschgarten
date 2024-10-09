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
)

@Service(Service.Level.PROJECT)
@State(
  name = "BspProjectProperties",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class BspProjectProperties : PersistentStateComponent<BspProjectPropertiesState> {
  var isBspProject: Boolean = false
  var isInitialized: Boolean = false
  var rootDir: VirtualFile? = null
  var buildToolId: BuildToolId? = null

  override fun getState(): BspProjectPropertiesState? =
    BspProjectPropertiesState(
      isBspProject = isBspProject,
      isInitialized = isInitialized,
      rootDirUrl = rootDir?.url,
      buildToolId = buildToolId?.id,
    )

  override fun loadState(state: BspProjectPropertiesState) {
    isBspProject = state.isBspProject
    isInitialized = state.isInitialized
    rootDir = state.rootDirUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    buildToolId = state.buildToolId?.let { BuildToolId(it) }
  }
}

public val Project.bspProjectProperties: BspProjectProperties
  get() = service<BspProjectProperties>()

public var Project.isBspProject: Boolean
  get() = bspProjectProperties.isBspProject
  set(value) {
    bspProjectProperties.isBspProject = value
  }

public var Project.isBspProjectInitialized: Boolean
  get() = bspProjectProperties.isInitialized
  set(value) {
    bspProjectProperties.isInitialized = value
  }

public var Project.rootDir: VirtualFile
  get() =
    bspProjectProperties.rootDir
      ?: error("BSP project root dir is not set. Reimport the project to fix this.")
  set(value) {
    bspProjectProperties.rootDir = value
  }

public var Project.buildToolId: BuildToolId
  get() =
    bspProjectProperties.buildToolId
      ?: error("Project's build tool id is not set. Reimport the project to fix this.")
  set(value) {
    bspProjectProperties.buildToolId = value
  }

public val Project.buildToolIdOrDefault: BuildToolId
  get() = bspProjectProperties.buildToolId ?: bspBuildToolId

public val Project.buildToolIdOrNull: BuildToolId?
  get() = bspProjectProperties.buildToolId
