package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.isBspProject

public class BspAndroidProjectSystemProvider(private val project: Project) : AndroidProjectSystemProvider {
  override val id: String = "org.jetbrains.plugins.bsp.android.BspAndroidProjectSystemProvider"

  override val projectSystem: AndroidProjectSystem
    get() = BspAndroidProjectSystem(project)

  override fun isApplicable(): Boolean = BspFeatureFlags.isAndroidSupportEnabled && project.isBspProject
}
