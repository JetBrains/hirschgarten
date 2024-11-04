package org.jetbrains.bsp.sdkcompat.flow.open

import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.project.Project

abstract class ExternalSystemUnlinkedProjectAwareCompat : ExternalSystemUnlinkedProjectAware {
  abstract suspend fun unlinkProjectCompat(project: Project, externalProjectPath: String)

  override suspend fun unlinkProject(project: Project, externalProjectPath: String) = unlinkProjectCompat(project, externalProjectPath)
}
