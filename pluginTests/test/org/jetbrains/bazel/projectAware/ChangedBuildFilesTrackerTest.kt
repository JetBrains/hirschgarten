package org.jetbrains.bazel.projectAware

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("ChangedBuildFilesTracker tests")
class ChangedBuildFilesTrackerTest : MockProjectBaseTest() {

  @Test
  fun `should track changed files`() {
    val tracker = ChangedBuildFilesTracker.getInstance(project)
    val path1 = Path("/workspace/foo/BUILD")
    val path2 = Path("/workspace/bar/BUILD.bazel")

    tracker.addChangedFile(path1)
    tracker.addChangedFile(path2)

    val result = tracker.consumeChangedFiles()
    result.shouldContainExactlyInAnyOrder(path1, path2)
  }

  @Test
  fun `consumeChangedFiles should clear the tracked files`() {
    val tracker = ChangedBuildFilesTracker.getInstance(project)
    tracker.addChangedFile(Path("/workspace/foo/BUILD"))

    tracker.consumeChangedFiles()
    val secondResult = tracker.consumeChangedFiles()
    secondResult.shouldBeEmpty()
  }

  @Test
  fun `should deduplicate same file added multiple times`() {
    val tracker = ChangedBuildFilesTracker.getInstance(project)
    val path = Path("/workspace/foo/BUILD")

    tracker.addChangedFile(path)
    tracker.addChangedFile(path)

    val result = tracker.consumeChangedFiles()
    result.size shouldBe 1
  }
}
