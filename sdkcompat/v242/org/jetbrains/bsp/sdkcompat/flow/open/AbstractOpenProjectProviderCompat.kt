package org.jetbrains.bsp.sdkcompat.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

abstract class AbstractOpenProjectProviderCompat : com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider() {
  open abstract fun systemIdCompat(): com.intellij.openapi.externalSystem.model.ProjectSystemId

  open abstract fun isProjectFileCompat(file: VirtualFile): Boolean

  open abstract suspend fun linkProjectCompat(projectFile: VirtualFile, project: Project)

  override val systemId = systemIdCompat()

  override fun isProjectFile(file: VirtualFile): Boolean = isProjectFileCompat(file)

  override suspend fun linkToExistingProjectAsync(projectFile: VirtualFile, project: Project) = linkProjectCompat(projectFile, project)
}
