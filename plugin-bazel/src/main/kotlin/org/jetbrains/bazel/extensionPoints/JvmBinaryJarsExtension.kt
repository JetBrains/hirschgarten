package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi

@PublicApi
interface JvmBinaryJarsExtension {
  fun shouldImportJvmBinaryJars(project: Project): Boolean

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<JvmBinaryJarsExtension> = create("org.jetbrains.bazel.jvmBinaryJarsExtension")
  }
}

@InternalApi
fun Project.shouldImportJvmBinaryJars(): Boolean = JvmBinaryJarsExtension.EP_NAME.extensions.any { it.shouldImportJvmBinaryJars(this) }
