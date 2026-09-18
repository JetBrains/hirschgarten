package org.jetbrains.bazel.import

import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MixedImportTest(bazelVersion: String) {
  @CcTestApplication
  class Bazel7 : MixedImportTest(BazelVersions.BAZEL_7)

  @CcTestApplication
  class Bazel8 : MixedImportTest(BazelVersions.BAZEL_8)

  private val project by clionBazelProjectFixture("import/mixed", jvmToolchains = true, bazelVersion = bazelVersion)

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testImportedTargets(): Unit = timeoutRunBlocking {
    project.findTarget("//cpp:library")
    project.findTarget("//kotlin:library")
  }
}
