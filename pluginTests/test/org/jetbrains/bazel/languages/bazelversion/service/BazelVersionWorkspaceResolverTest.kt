package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.util.text.SemVer
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test

class BazelVersionWorkspaceResolverTest {
  @Test
  fun `test workspace version resolver bazelisk`() {
    val path = BazelPathManager.getTestFixturePath("bazelversion/version_resolve_bazelisk")
    val version = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(path)
    version!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("123.4.10")!!)
  }

  @Test
  fun `test workspace version resolver bazelversion`() {
    val path = BazelPathManager.getTestFixturePath("bazelversion/version_resolve_bazelversion")
    val version = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(path)
    version!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("123.4.5")!!)
  }
}
