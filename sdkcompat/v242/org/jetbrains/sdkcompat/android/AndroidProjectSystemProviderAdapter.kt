package org.jetbrains.sdkcompat.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider

// v243: fun isApplicable() is changed to fun isApplicable(Project)
// v243: val projectSystem is changed to fun projectSystemFactory(Project)
abstract class AndroidProjectSystemProviderAdapter : AndroidProjectSystemProvider {
  abstract fun isApplicableCompat(): Boolean

  abstract val projectSystemCompat: AndroidProjectSystem

  override val projectSystem: AndroidProjectSystem
    get() = projectSystemCompat

  override fun isApplicable(): Boolean = isApplicableCompat()
}
