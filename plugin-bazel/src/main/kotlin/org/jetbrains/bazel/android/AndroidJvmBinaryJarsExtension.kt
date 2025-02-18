package org.jetbrains.bazel.android

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.extensionPoints.JvmBinaryJarsExtension

class AndroidJvmBinaryJarsExtension : JvmBinaryJarsExtension {
  override fun shouldImportJvmBinaryJars(project: Project): Boolean = BspFeatureFlags.isAndroidSupportEnabled
}
