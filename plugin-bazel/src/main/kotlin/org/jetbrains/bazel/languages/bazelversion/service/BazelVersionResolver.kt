package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral

@PublicApi
interface BazelVersionResolver {
  val id: String
  val name: String

  suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): String?

  companion object {
    val ep = ExtensionPointName.create<BazelVersionResolver>("org.jetbrains.bazel.bazelVersionResolver")
  }
}
