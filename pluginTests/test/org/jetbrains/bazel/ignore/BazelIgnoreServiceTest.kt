package org.jetbrains.bazel.ignore

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.testRepoMapping
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelIgnoreServiceTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `test multiple repositories`() {
    testBazel.writeFile(
      ".bazelignore",
      """
        sources
        nested
      """.trimIndent(),
    )
    testBazel.writeFile(
      "nested/.bazelignore",
      """
        src
      """.trimIndent(),
    )

    val project = testBazel.project
    val workspaceRoot = testBazel.workspaceRoot
    (BazelIgnoreService.getInstance(project) as DefaultBazelIgnoreService)
      .update(
        workspaceRoot,
        testRepoMapping(testBazel.bazelInfo) { local("nested+", "nested") },
      )

    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot).shouldBeFalse()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("sources")).shouldBeTrue()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("nested")).shouldBeFalse()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("nested/src")).shouldBeTrue()
  }
}
