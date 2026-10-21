package org.jetbrains.bazel.redcodes

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.isWorkspace
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

/**
 * The project has a broken `bazel_dep` on `grpc-java`, and no target uses it.
 * The sync must still import the workspace target.
 */
@BazelTestApplication
class BrokenDepsTest {

  private val project by bazelProjectFixture("redcodes/broken_deps")

  @Test
  fun `sync succeeds in the presence of broken dependencies`() {
    val targetStorage = project.targetStorage
    val repoMapping = targetStorage.repoMapping()
    val workspaceTargets = targetStorage.allTargetSummaries().asSequence()
      .filter { it.isWorkspace(repoMapping) }
      .map { it.id }
      .toSet()

    workspaceTargets shouldBe setOf(Label.parse("//:hello"))
  }
}
