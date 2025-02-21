package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.annotations.PublicApi

@PublicApi
interface JvmBinaryJarsExtension {
  fun shouldImportJvmBinaryJars(project: Project): Boolean

  companion object {
    val EP_NAME: ExtensionPointName<JvmBinaryJarsExtension> = create("org.jetbrains.bazel.jvmBinaryJarsExtension")
  }
}

@ApiStatus.Internal
fun Project.shouldImportJvmBinaryJars(): Boolean = JvmBinaryJarsExtension.EP_NAME.extensions.any { it.shouldImportJvmBinaryJars(this) }
