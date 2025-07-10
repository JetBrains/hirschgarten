package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewSectionItemCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should complete workspace type variants`() {
    myFixture.configureByText(".bazelproject", "workspace_type: <caret>")
    myFixture.type("a")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldContainAll listOf("java", "javascript", "dart", "android")
  }

  @Test
  fun `should complete build flags variants`() {
    myFixture.configureByText(".bazelproject", "build_flags:\n  <caret>")
    myFixture.type("action")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    val expectedFlags = Flag.all().filter { it.key.contains("action") }.map { it.key }

    lookups shouldContainAll expectedFlags
  }

  @Test
  fun `should complete build flags variants with existing flag`() {
    myFixture.configureByText(".bazelproject", "build_flags:\n  --action_cache\n  <caret>")
    myFixture.type("action")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
    lookups shouldNotContain "--action_cache"
  }

  @Test
  fun `should complete boolean sections`() {
    myFixture.configureByText(".bazelproject", "use_query_sync: <caret>")
    myFixture.type("t")
    myFixture.completeBasic()
    
    myFixture.checkResult("use_query_sync: true")
  }
}
