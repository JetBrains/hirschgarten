package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcHeaderOwnershipTest {

  private val project by clionBazelProjectFixture("clion/header_ownership", buildProject = true, bazelVersion = BazelVersions.BAZEL_9)

  @Test
  fun testSourceHasItsTarget(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/lib.cc")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInSrcsHasItsTarget(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/private.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInHdrsHasItsTarget(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/lib.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testTextualHeaderHasItsTarget(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/colors.inl")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInSrcsAndHdrsHasItsTargetOnce(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/shared.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testFileOfNoTargetHasNoTarget(): Unit = timeoutRunBlocking {
    assertThat(targetsFor("lib/orphan.h")).isEmpty()
  }

  private fun targetsFor(relativePath: String): List<Label> {
    val file = project.rootDir.findFileByRelativePath(relativePath).assertNotNull()
    return project.targetStorage.getTargetsForFile(file)
  }
}
