package org.jetbrains.plugins.bsp.android

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.extensionPoints.JvmBinaryJarsExtension

class AndroidJvmBinaryJarsExtension : JvmBinaryJarsExtension {
  override fun shouldImportJvmBinaryJars(project: Project): Boolean = BspFeatureFlags.isAndroidSupportEnabled
}
