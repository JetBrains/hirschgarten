package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * An excluded directory that holds no Bazel package must not make the sync fail.
 * With `derive_targets_from_directories`, the sync turns the excluded `docs` directory into the `-//docs/...` pattern.
 * See BAZEL-3586.
 */
class ExcludedNonPackageDirectoryTest {

  /** One aspect build gets all targets. */
  @Nested
  @BazelTestApplication
  inner class Monolithic {
    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/excluded_non_package_directory",
      projectView = ".bazelproject",
    )

    @Test
    fun testSyncSucceeds(): Unit = runBlocking(Dispatchers.Default) {
      fixture.checkSync()
    }
  }

  /** `expand_and_shard` with one target per shard passes the excluded patterns to each shard build. */
  @Nested
  @BazelTestApplication
  inner class Sharded {
    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/excluded_non_package_directory",
      projectView = "sharded.bazelproject",
    )

    @Test
    fun testShardedSyncSucceeds(): Unit = runBlocking(Dispatchers.Default) {
      fixture.checkSync()
    }
  }
}

/** The sync must end without an error, and both packages must be imported. */
private suspend fun BazelSyncCodeInsightTestFixture.checkSync() {
  assertLastSyncSucceeded(project)
  withContext(Dispatchers.EDT) {
    checkHighlighting("lib/Lib.java")
    checkHighlighting("app/App.java")
  }
}
