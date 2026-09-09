package org.jetbrains.bazel.import

import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MixedImportTest {

  private val project by clionBazelProjectFixture("import/mixed", jvmToolchains = true)

  @Test
  @Disabled("Correctly reports that we do cause indexing in bazel-bin for this project.")
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testImportedTargets(): Unit = timeoutRunBlocking {
    project.findTarget("//cpp:library")
    project.findTarget("//kotlin:library")
  }
}
