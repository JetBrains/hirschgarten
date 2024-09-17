package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.isBspProject

public class BspAndroidProjectSystemProvider : AndroidProjectSystemProvider {
  override val id: String = "org.jetbrains.plugins.bsp.android.BspAndroidProjectSystemProvider"

  override fun isApplicable(project: Project): Boolean = BspFeatureFlags.isAndroidSupportEnabled && project.isBspProject

  override fun projectSystemFactory(project: Project): AndroidProjectSystem = BspAndroidProjectSystem(project)
}
