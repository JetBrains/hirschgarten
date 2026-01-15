package org.jetbrains.bazel.redcodes

import com.intellij.lang.annotation.HighlightSeverity
import io.kotest.matchers.collections.shouldHaveSingleElement
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightFixtureTestCase
import org.jetbrains.bazel.test.framework.doHighlighting

class KotlinModuleInternalManglingTest : BazelSyncCodeInsightFixtureTestCase() {

  fun testNoErrors() {
    myFixture.copyDirectoryToProject("redcodes/kotlin_module_internal_mangling", "")
    myFixture.performBazelSync()
    myFixture
      .doHighlighting("B.java", HighlightSeverity.ERROR)
      .shouldHaveSingleElement {
        it.text == $$"method$wrong_module" && it.description == $$"Cannot resolve method 'method$wrong_module' in 'A'"
      }
  }
}
