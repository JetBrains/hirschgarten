package org.jetbrains.bazel.clion

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.fixtures.CC_TEST_BAZEL_INFO
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.junit.jupiter.api.Test

class CcFileToTargetMapTest {

  private val snapshot = ccProject {
    val lib = ccLibrary {
      label("//lib:lib")
      srcs("lib/lib.cc", "lib/private.h", "lib/shared.h")
      hdrs("lib/lib.h", "lib/shared.h")
      textualHdrs("lib/colors.inl")
    }

    ccBinary {
      label("//app:app")
      srcs("app/main.cc")
      deps(lib)
    }
  }

  @Test
  fun testSourceHasItsTarget() {
    assertThat(snapshot.labelsFor("lib/lib.cc")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInSrcsHasItsTarget() {
    assertThat(snapshot.labelsFor("lib/private.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInHdrsHasItsTarget() {
    assertThat(snapshot.labelsFor("lib/lib.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testTextualHeaderHasItsTarget() {
    assertThat(snapshot.labelsFor("lib/colors.inl")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testHeaderInSrcsAndHdrsHasItsTargetOnce() {
    assertThat(snapshot.labelsFor("lib/shared.h")).containsExactly(Label.parse("//lib:lib"))
  }

  @Test
  fun testFileOfNoTargetHasNoTarget() {
    assertThat(snapshot.labelsFor("lib/orphan.h")).isEmpty()
  }

  private fun WorkspaceSnapshot.labelsFor(relativePath: String): List<Label> {
    return fileToTarget.getTargetsByFile(CC_TEST_BAZEL_INFO.workspaceRoot.resolve(relativePath)).map { it.label }
  }
}
