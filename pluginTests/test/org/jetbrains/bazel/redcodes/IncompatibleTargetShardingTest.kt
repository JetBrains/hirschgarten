package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * A sharded sync must keep the Bazel selection semantics of a wildcard pattern.
 * Bazel skips a platform-incompatible target that a wildcard selects, so the sync must not fail on it.
 * See BAZEL-3552.
 */
class IncompatibleTargetShardingTest {

  /** `query_and_shard` expands `//...` into explicit labels before the shard build. */
  @Nested
  @BazelTestApplication
  inner class QueryAndShard {
    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/incompatible_target_sharding",
      projectView = "query_and_shard.bazelproject",
    )

    @Test
    fun testIncompatibleTargetIsSkipped(): Unit = runBlocking(Dispatchers.Default) {
      fixture.checkSync()
    }
  }

  /** `expand_and_shard` expands `//...` into package wildcards, and then into explicit labels. */
  @Nested
  @BazelTestApplication
  inner class ExpandAndShard {
    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/incompatible_target_sharding",
      projectView = "expand_and_shard.bazelproject",
    )

    @Test
    fun testIncompatibleTargetIsSkipped(): Unit = runBlocking(Dispatchers.Default) {
      fixture.checkSync()
    }
  }

  /** The control: the same project without sharding passes `//...` to Bazel as is. */
  @Nested
  @BazelTestApplication
  inner class Unsharded {
    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/incompatible_target_sharding",
      projectView = "unsharded.bazelproject",
    )

    @Test
    fun testIncompatibleTargetIsSkipped(): Unit = runBlocking(Dispatchers.Default) {
      fixture.checkSync()
    }

    @Test
    fun testPartialSyncSkipsIncompatibleTarget(): Unit = runBlocking(Dispatchers.Default) {
      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//:incompatible")), build = false))
      fixture.checkSync()
    }
  }
}

/** The sync must end without an error, and the compatible target must be imported. */
private suspend fun BazelSyncCodeInsightTestFixture.checkSync() {
  assertLastSyncSucceeded(project)
  withContext(Dispatchers.EDT) {
    checkHighlighting("Lib.java")
  }
}
