package org.jetbrains.bazel.redcodes

import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestCase

class KotlinModuleInternalManglingTest : BazelSyncCodeInsightTestCase() {

  fun testNoErrors() {
    myFixture.copyDirectoryToProject("redcodes/kotlin_module_internal_mangling", "")
    myFixture.performBazelSync()
    myFixture.testHighlighting("B.java")
  }
}
