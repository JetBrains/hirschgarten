package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral

@InternalApi
interface BazelVersionResolver {
  val id: String
  val name: String

  suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): BazelVersionLiteral?

  companion object {
    val ep = ExtensionPointName.create<BazelVersionResolver>("org.jetbrains.bazel.bazelVersionResolver")
  }
}
