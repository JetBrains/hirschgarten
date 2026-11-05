package org.jetbrains.bazel.redcodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class SameClassInTestTest {
  private val fixture by bazelSyncCodeInsightFixture("redcodes/same_class_in_test", buildProject = true)

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.checkHighlighting("src/java/com/example/Production.java")
  }
}
