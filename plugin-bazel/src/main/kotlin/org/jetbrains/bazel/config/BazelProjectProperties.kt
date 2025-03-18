package org.jetbrains.bazel.config

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import kotlinx.serialization.Serializable
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.workspacemodel.entities.BspProjectDirectoriesEntity

@Serializable
data class BazelProjectPropertiesState(
  val isBazelProject: Boolean = false,
  val isInitialized: Boolean = false,
  val rootDir: VirtualFile? = null,
  val defaultJdkName: String? = null,
  val isBrokenBspProject: Boolean = false,
)

@Service(Service.Level.PROJECT)
@State(
  name = "BazelProjectProperties",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class BazelProjectProperties(private val project: Project) : SerializablePersistentStateComponent<BazelProjectPropertiesState>(BazelProjectPropertiesState()) {

  override fun noStateLoaded() {
    // See https://youtrack.jetbrains.com/issue/BAZEL-1500.
    // It is possible that the user deleted .idea/workspace.xml, but the workspace model cache is left intact.
    // In that case, we can know if we previously imported the project when BspProjectDirectoriesEntity exists.
    val bspProjectDirectories = getBspProjectDirectoriesEntity() ?: return
    updateState { it.copy(
      rootDir = bspProjectDirectories.projectRoot.virtualFile ?: return,
      isBrokenBspProject = true
      )
    }
  }

  private fun getBspProjectDirectoriesEntity(): BspProjectDirectoriesEntity? =
    WorkspaceModel
      .getInstance(project)
      .currentSnapshot
      .entities(BspProjectDirectoriesEntity::class.java)
      .firstOrNull()
}

val Project.bazelProjectProperties: BazelProjectProperties
  get() = service<BazelProjectProperties>()


var Project.isBazelProject: Boolean
  get() = bazelProjectProperties.state.isBazelProject
  set(value) {
    bazelProjectProperties.state = bazelProjectProperties.state.copy(isBazelProject = value)
  }

var Project.isBazelProjectInitialized: Boolean
  get() = bazelProjectProperties.state.isInitialized
  set(value) {
    bazelProjectProperties.state = bazelProjectProperties.state.copy(isInitialized = value)
  }

val Project.workspaceModelLoadedFromCache: Boolean
  get() = (workspaceModel as WorkspaceModelImpl).loadedFromCache

val Project.isBrokenBspProject: Boolean
  get() = bazelProjectProperties.state.isBrokenBspProject

@PublicApi
var Project.rootDir: VirtualFile
  get() =
    bazelProjectProperties.state.rootDir
      ?: error("Bazel project root dir is not set. Reimport the project to fix this.")
  set(value) {
    bazelProjectProperties.state = bazelProjectProperties.state.copy(rootDir = value)
  }

val Project.bazelProjectName: String
  get() = rootDir.name

var Project.defaultJdkName: String?
  get() = bazelProjectProperties.state.defaultJdkName
  set(value) {
    bazelProjectProperties.state = bazelProjectProperties.state.copy(
      defaultJdkName = value
    )
  }
