package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.magicmetamodel.impl.ConvertableToState

public data class ProjectProperties(
  public val isBspProject: Boolean,
  public val projectRootDir: VirtualFile
) : ConvertableToState<ProjectPropertiesState> {

  override fun toState(): ProjectPropertiesState =
    ProjectPropertiesState(
      isBspProject = isBspProject,
      projectRootDir = projectRootDir.url
    )

  public companion object {
    public fun fromState(state: ProjectPropertiesState): ProjectProperties =
      ProjectProperties(
        isBspProject = state.isBspProject,
        projectRootDir = state.projectRootDir?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
          ?: throw IllegalStateException("projectRootDir should be set! Probably state has been corrupted")
      )
  }
}

public data class ProjectPropertiesState(
  public var isBspProject: Boolean = false,
  public var projectRootDir: String? = null,
)

@State(
  name = "ProjectPropertiesService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true
)
public class ProjectPropertiesService : PersistentStateComponent<ProjectPropertiesState> {

  public lateinit var projectProperties: ProjectProperties

  override fun getState(): ProjectPropertiesState =
    projectProperties.toState()

  override fun loadState(state: ProjectPropertiesState) {
    projectProperties = ProjectProperties.fromState(state)
  }

  public companion object {
    public fun getInstance(project: Project): ProjectPropertiesService =
      project.getService(ProjectPropertiesService::class.java)
  }
}
