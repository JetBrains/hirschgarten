package org.jetbrains.bazel.android

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.extensionPoints.JvmBinaryJarsExtension

class AndroidJvmBinaryJarsExtension : JvmBinaryJarsExtension {
  override fun shouldImportJvmBinaryJars(project: Project): Boolean = BazelFeatureFlags.isAndroidSupportEnabled
}
