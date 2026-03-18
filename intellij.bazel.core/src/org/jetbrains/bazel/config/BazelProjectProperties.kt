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
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity

@ApiStatus.Internal
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
@ApiStatus.Internal
class BazelProjectProperties(private val project: Project) : PersistentStateComponent<BazelProjectPropertiesState> {
  var isBazelProject: Boolean = false
  var rootDir: VirtualFile? = null
  var defaultJdkName: String? = null
  var isBrokenBazelProject: Boolean = false
  var workspaceName: String? = null

  // todo the whole class should be removed
  //   because almost all the entities duplicate already existing platform entities

  override fun getState(): BazelProjectPropertiesState =
    BazelProjectPropertiesState(
      isBazelProject = isBazelProject,
      rootDirUrl = rootDir?.url,
      defaultJdkName = defaultJdkName,
    )

  override fun loadState(state: BazelProjectPropertiesState) {
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
    isBrokenBazelProject = true
  }

  private fun getBazelProjectDirectoriesEntity(): BazelProjectDirectoriesEntity? =
    WorkspaceModel
      .getInstance(project)
      .currentSnapshot
      .entities(BazelProjectDirectoriesEntity::class.java)
      .firstOrNull()
}

val Project.bazelProjectProperties: BazelProjectProperties
  @ApiStatus.Internal
  get() = service<BazelProjectProperties>()

// todo replace with an instanceof BazelProjectStoreDescriptor check in some way
var Project.isBazelProject: Boolean
  get() = projectCtx.isBazelProject
  @ApiStatus.Internal
  set(value) {
    projectCtx.isBazelProject = value
  }

internal val Project.workspaceModelLoadedFromCache: Boolean
  get() = (workspaceModel as WorkspaceModelImpl).loadedFromCache

internal val Project.isBrokenBazelProject: Boolean
  get() = bazelProjectProperties.isBrokenBazelProject

var Project.rootDir: VirtualFile
  get() = projectCtx.getProjectRootDirOrThrow()
  @ApiStatus.Internal
  set(value) {
    projectCtx.projectRootDir = value
  }

internal val Project.bazelProjectName: String
  get() = rootDir.name

var Project.defaultJdkName: String?
  @ApiStatus.Internal
  get() = bazelProjectProperties.defaultJdkName
  @ApiStatus.Internal
  set(value) {
    bazelProjectProperties.defaultJdkName = value
  }

internal var Project.workspaceName: String?
  get() = projectCtx.workspaceName
  set(value) {
    projectCtx.workspaceName = value
  }
