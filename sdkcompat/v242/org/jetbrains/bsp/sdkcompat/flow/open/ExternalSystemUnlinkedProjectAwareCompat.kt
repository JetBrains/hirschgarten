package org.jetbrains.bsp.sdkcompat.flow.open

import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.project.Project

abstract class ExternalSystemUnlinkedProjectAwareCompat : ExternalSystemUnlinkedProjectAware {
  // this feature is not available in v242, only from 243
  abstract suspend fun unlinkProjectCompat(project: Project, externalProjectPath: String)
}
