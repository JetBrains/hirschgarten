package org.jetbrains.sdkcompat.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider
import com.intellij.openapi.project.Project

// v243: fun isApplicable() is changed to fun isApplicable(Project)
// v243: val projectSystem is changed to fun projectSystemFactory(Project)
abstract class AndroidProjectSystemProviderAdapter : AndroidProjectSystemProvider {
  abstract fun isApplicableCompat(): Boolean

  abstract val projectSystemCompat: AndroidProjectSystem

  override fun projectSystemFactory(project: Project): AndroidProjectSystem {
    return projectSystemCompat
  }

  override fun isApplicable(project: Project): Boolean {
    return isApplicableCompat()
  }
}
