package org.jetbrains.bazel.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity

data class BazelProjectPropertiesState(
  var isBazelProject: Boolean = false,
  var isInitialized: Boolean = false,
  var rootDirUrl: String? = null,
  var buildToolId: String? = null,
  var defaultJdkName: String? = null,
)

@Service(Service.Level.PROJECT)
@State(
  name = "BazelProjectProperties",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class BazelProjectProperties(private val project: Project) : PersistentStateComponent<BazelProjectPropertiesState> {
  var isInitialized: Boolean = false
  var isBazelProject: Boolean = false
  var rootDir: VirtualFile? = null
  var defaultJdkName: String? = null
  var isBrokenBspProject: Boolean = false
  var workspaceName: String? = null

  override fun getState(): BazelProjectPropertiesState? =
    BazelProjectPropertiesState(
      isInitialized = isInitialized,
      isBazelProject = isBazelProject,
      rootDirUrl = rootDir?.url,
      defaultJdkName = defaultJdkName,
    )

  override fun loadState(state: BazelProjectPropertiesState) {
    isInitialized = state.isInitialized
    isBazelProject = state.isBazelProject
    rootDir = state.rootDirUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    defaultJdkName = state.defaultJdkName
  }

  override fun noStateLoaded() {
    // See https://youtrack.jetbrains.com/issue/BAZEL-1500.
    // It is possible that the user deleted .idea/workspace.xml, but the workspace model cache is left intact.
    // In that case, we can know if we previously imported the project when [BazelProjectDirectoriesEntity] exists.
    val bazelProjectDirectories = getBazelProjectDirectoriesEntity() ?: return
    rootDir = bazelProjectDirectories.projectRoot.virtualFile ?: return
    isBrokenBspProject = true
  }

  private fun getBazelProjectDirectoriesEntity(): BazelProjectDirectoriesEntity? =
    WorkspaceModel
      .getInstance(project)
      .currentSnapshot
      .entities(BazelProjectDirectoriesEntity::class.java)
      .firstOrNull()
}

val Project.bazelProjectProperties: BazelProjectProperties
  get() = service<BazelProjectProperties>()

@PublicApi
var Project.isBazelProject: Boolean
  get() = bazelProjectProperties.isBazelProject
  set(value) {
    bazelProjectProperties.isBazelProject = value
  }

var Project.isBazelProjectInitialized: Boolean
  get() = bazelProjectProperties.isInitialized
  set(value) {
    bazelProjectProperties.isInitialized = value
  }

val Project.isBazelProjectLoaded: Boolean
  get() = isBazelProjectInitialized && workspaceModelLoadedFromCache

val Project.workspaceModelLoadedFromCache: Boolean
  get() = (workspaceModel as WorkspaceModelImpl).loadedFromCache

val Project.isBrokenBspProject: Boolean
  get() = bazelProjectProperties.isBrokenBspProject

@PublicApi
var Project.rootDir: VirtualFile
  get() =
    bazelProjectProperties.rootDir
      ?: error("Bazel project root dir is not set. Reimport the project to fix this.")
  set(value) {
    bazelProjectProperties.rootDir = value
  }

val Project.bazelProjectName: String
  get() = rootDir.name

var Project.defaultJdkName: String?
  get() = bazelProjectProperties.defaultJdkName
  set(value) {
    bazelProjectProperties.defaultJdkName = value
  }

var Project.workspaceName: String?
  get() = bazelProjectProperties.workspaceName
  set(value) {
    bazelProjectProperties.workspaceName = value
  }
