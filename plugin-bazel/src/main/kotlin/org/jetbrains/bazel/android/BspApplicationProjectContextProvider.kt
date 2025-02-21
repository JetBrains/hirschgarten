package org.jetbrains.bazel.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.ApplicationProjectContext
import com.android.tools.idea.projectsystem.ApplicationProjectContextProvider

class BspApplicationProjectContextProvider : ApplicationProjectContextProvider<BspAndroidProjectSystem> {
  override fun computeApplicationProjectContext(
    projectSystem: BspAndroidProjectSystem,
    client: ApplicationProjectContextProvider.RunningApplicationIdentity,
  ): ApplicationProjectContext? {
    val applicationId = client.applicationId ?: return null
    return object : ApplicationProjectContext {
      override val applicationId: String = applicationId
    }
  }

  override fun isApplicable(projectSystem: AndroidProjectSystem): Boolean = projectSystem is BspAndroidProjectSystem
}
