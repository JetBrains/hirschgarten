package org.jetbrains.bazel.languages.starlark.bazel.modules

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.annotations.InternalApi

@InternalApi
interface BazelModuleResolver {
  val id: String
  val name: String

  suspend fun getModuleNames(project: Project): List<String>?

  suspend fun getModuleVersions(project: Project, moduleName: String): List<String>?

  suspend fun refreshModuleNames(project: Project)

  fun clearCache(project: Project)

  fun getCachedModuleNames(project: Project): List<String>

  fun getCachedModuleVersions(project: Project, moduleName: String): List<String>

  companion object {
    val EP_NAME: ExtensionPointName<BazelModuleResolver> = ExtensionPointName.create("org.jetbrains.bazel.bazelModuleResolver")
  }
}
