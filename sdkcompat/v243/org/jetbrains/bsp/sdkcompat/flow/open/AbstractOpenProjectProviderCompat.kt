package org.jetbrains.bsp.sdkcompat.flow.open

import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

abstract class AbstractOpenProjectProviderCompat : AbstractOpenProjectProvider() {
  open abstract fun systemIdCompat(): ProjectSystemId

  open abstract fun isProjectFileCompat(file: VirtualFile): Boolean

  open abstract suspend fun linkProjectCompat(projectFile: VirtualFile, project: Project)

  override val systemId = systemIdCompat()

  override fun isProjectFile(file: VirtualFile): Boolean = isProjectFileCompat(file)

  override suspend fun linkProject(projectFile: VirtualFile, project: Project) = linkProjectCompat(projectFile, project)
}
