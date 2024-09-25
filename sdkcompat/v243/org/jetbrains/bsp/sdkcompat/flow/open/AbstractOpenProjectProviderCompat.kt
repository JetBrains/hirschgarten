package org.jetbrains.bsp.sdkcompat.flow.open

import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// v243: linkToExistingProjectAsync renamed to linkProject
abstract class AbstractOpenProjectProviderCompat : AbstractOpenProjectProvider() {
  abstract fun systemIdCompat(): ProjectSystemId

  abstract fun isProjectFileCompat(file: VirtualFile): Boolean

  abstract suspend fun linkProjectCompat(projectFile: VirtualFile, project: Project)

  override val systemId = systemIdCompat()

  override fun isProjectFile(file: VirtualFile): Boolean = isProjectFileCompat(file)

  override suspend fun linkProject(projectFile: VirtualFile, project: Project) = linkProjectCompat(projectFile, project)
}
