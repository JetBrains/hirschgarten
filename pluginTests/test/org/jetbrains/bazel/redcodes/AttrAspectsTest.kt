package org.jetbrains.bazel.redcodes

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.isWorkspace
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

@BazelTestApplication
class AttrAspectsTest {

  private val project by bazelProjectFixture("redcodes/attr_aspects", projectView = ".bazelproject")

  @Test
  fun `attr_aspects in project view is honored`() {
    val targetStorage = project.targetStorage
    val repoMapping = targetStorage.repoMapping()
    val workspaceTargets = targetStorage.allTargetSummaries().asSequence()
      .filter { it.isWorkspace(repoMapping) }
      .map { it.id }
      .toSet()

    workspaceTargets shouldBe setOf(Label.parse("//lib:lib"), Label.parse("//app:app_with_data"))
  }
}
