package org.jetbrains.bazel.redcodes

import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightFixtureTestCase

class KotlinModuleInternalManglingTest : BazelSyncCodeInsightFixtureTestCase() {

  fun testNoErrors() {
    myFixture.copyDirectoryToProject("redcodes/kotlin_module_internal_mangling", "")
    myFixture.performBazelSync()
    myFixture.testHighlighting("B.java")
  }
}
