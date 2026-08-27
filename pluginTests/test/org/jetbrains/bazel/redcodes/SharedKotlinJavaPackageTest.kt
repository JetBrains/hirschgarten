package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test


@BazelTestApplication
class SharedKotlinJavaPackageTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/shared_kotlin_java_package",
  )

  @Test
  fun testMatchingDirectoryLayoutHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("matching_dirs/src/main/kotlin/foo/KotlinClassA.kt")
      fixture.checkHighlighting("matching_dirs/src/main/kotlin/foo/KotlinClassB.kt")
      // Java file must not be first, so it's not selected by java source root optimization
      // We need that to reproduce the issue
      fixture.checkHighlighting("matching_dirs/src/main/kotlin/foo/ZJavaClass.java")
    }
  }

  @Test
  fun testNonMatchingDirectoryLayoutHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("non_matching_dirs/src/KotlinClassA.kt")
      fixture.checkHighlighting("non_matching_dirs/src/KotlinClassB.kt")
      // Java file must not be first, so it's not selected by java source root optimization
      // We need that to reproduce the issue
      fixture.checkHighlighting("non_matching_dirs/src/ZJavaClass.java")
    }
  }
}
