package org.jetbrains.bazel.languages.bazelversion.service

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.util.text.SemVer
import io.kotest.matchers.equals.shouldBeEqual
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.junit.jupiter.api.Test

class BazelVersionWorkspaceResolverTest {

  @Test
  fun `test workspace version resolver bazelisk`() {
    val path = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelversion/version_resolve_bazelisk")
    val version = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(path)
    version!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("123.4.10")!!)
  }

  @Test
  fun `test workspace version resolver bazelversion`() {
    val path = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelversion/version_resolve_bazelversion")
    val version = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(path)
    version!! shouldBeEqual BazelVersionLiteral.Specific(SemVer.parseFromText("123.4.5")!!)
  }
}
