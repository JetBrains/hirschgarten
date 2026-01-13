package org.jetbrains.bazel.redcodes

import org.jetbrains.bazel.test.framework.BazelSyncingTestCase

class KotlinModuleInternalManglingTest : BazelSyncingTestCase() {

  fun testNoErrors() {
    copyToProjectRootFromTestData("redcodes/kotlin_module_internal_mangling")
    performSync()
    assertNoErrorsHighlighted(projectRootPath.resolve("B.java"))
  }
}
