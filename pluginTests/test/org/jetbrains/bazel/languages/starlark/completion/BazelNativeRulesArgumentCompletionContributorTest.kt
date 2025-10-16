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
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("e")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

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
    myFixture.configureByText("BUILD", "java_binary(name = \"\", <caret>)")
    myFixture.type("e")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldNotContain listOf("name")
  }

  @Test
  fun `should complete empty list`() {
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("src")

    myFixture.completeBasic()

    myFixture.checkResult("java_binary(srcs = [<caret>],)")
  }

  @Test
  fun `should complete empty string`() {
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("resource_strip_prefi")

    myFixture.completeBasic()

    myFixture.checkResult("java_binary(resource_strip_prefix = \"<caret>\",)")
  }
}
