package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.services.ValueServiceWhichNeedsToBeInitialized

public data class ProjectPropertiesState(
  var projectRootDir: String? = null
)

public data class ProjectProperties(
  val projectRootDir: VirtualFile
) : ConvertableToState<ProjectPropertiesState> {

  override fun toState(): ProjectPropertiesState =
    ProjectPropertiesState(
      projectRootDir = projectRootDir.url,
    )

  public companion object {
    public fun fromState(state: ProjectPropertiesState): ProjectProperties =
      ProjectProperties(
        projectRootDir = state.projectRootDir?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
          ?: error("Can't parse the state! `projectRootDir` can't be null."),
      )
  }
}

@State(
  name = "ProjectPropertiesService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true
)
public class ProjectPropertiesService :
  ValueServiceWhichNeedsToBeInitialized<ProjectProperties>(),
  PersistentStateComponent<ProjectPropertiesState> {

  override fun getState(): ProjectPropertiesState =
    value.toState()

  override fun loadState(state: ProjectPropertiesState) {
    value = ProjectProperties.fromState(state)
  }

  public companion object {
    public fun getInstance(project: Project): ProjectPropertiesService =
      project.getService(ProjectPropertiesService::class.java)
  }
}
