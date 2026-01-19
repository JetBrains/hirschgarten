package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import io.kotest.common.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightFixtureTestCase
import org.jetbrains.bazel.test.framework.checkHighlighting

class KotlinModuleInternalManglingTest : BazelSyncCodeInsightFixtureTestCase() {

  fun testHighlighting() = runBlocking {
    myFixture.copyDirectoryToProject("redcodes/kotlin_module_internal_mangling", "")
    myFixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      myFixture.checkHighlighting("B.java")
    }
  }
}
