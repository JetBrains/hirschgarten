package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelNativeRulesArgumentCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should complete native rule arguments`() {
    // given
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("e")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll
      listOf(
        "name",
        "deps",
        "resources",
        "add_opens",
        "classpath_resources",
        "deploy_env",
        "deploy_manifest_lines",
      )
  }

  @Test
  fun `should not double complete native rule arguments`() {
    // given
    myFixture.configureByText("BUILD", "java_binary(name = \"\", <caret>)")
    myFixture.type("e")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldNotContain listOf("name")
  }

  @Test
  fun `should complete empty list`() {
    // given
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("src")

    // when
    myFixture.completeBasic()

    // then
    myFixture.checkResult("java_binary(srcs = [<caret>],)")
  }

  @Test
  fun `should complete empty string`() {
    // given
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("resource_strip_prefi")

    // when
    myFixture.completeBasic()

    // then
    myFixture.checkResult("java_binary(resource_strip_prefix = \"<caret>\",)")
  }
}
