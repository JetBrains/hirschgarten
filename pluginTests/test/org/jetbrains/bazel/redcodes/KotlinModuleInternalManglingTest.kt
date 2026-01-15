package org.jetbrains.bazel.redcodes

import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightFixtureTestCase
import org.jetbrains.bazel.test.framework.checkHighlighting

class KotlinModuleInternalManglingTest : BazelSyncCodeInsightFixtureTestCase() {

  fun testNoErrors() {
    myFixture.copyDirectoryToProject("redcodes/kotlin_module_internal_mangling", "")
    myFixture.performBazelSync()
    myFixture.checkHighlighting("B.java")
  }
}
