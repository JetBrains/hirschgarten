package org.jetbrains.bazel.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBspProject

public class BspAndroidProjectSystemProvider : AndroidProjectSystemProvider {
  override val id: String = "org.jetbrains.bazel.android.BspAndroidProjectSystemProvider"

  override fun isApplicable(project: Project): Boolean = BazelFeatureFlags.isAndroidSupportEnabled && project.isBspProject

  override fun projectSystemFactory(project: Project): AndroidProjectSystem = BspAndroidProjectSystem(project)
}
