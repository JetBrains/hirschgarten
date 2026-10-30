package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelNativeRulesArgumentCompletionContributorTest {
  private val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1))

  @Test
  fun `should complete native rule arguments`() {
    myFixture.configureByText("BUILD", "java_binary(<caret>)")
    myFixture.type("e")

    val lookups = myFixture.completeBasicLookupStrings()

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

    val lookups = myFixture.completeBasicLookupStrings()

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
