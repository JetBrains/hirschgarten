package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.sync
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelGlobalFunctionArgumentCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should complete module function`() {
    myFixture.configureByText("MODULE.bazel", "module(<caret>)")
    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings()

    val expectedArgNames =
      BazelGlobalFunctions.moduleGlobalFunctions(myFixture.project)["module"]!!
        .params
        .filter { it.name.contains('a') }
        .map { it.name }
    lookups shouldContainAll expectedArgNames
  }

  @Test
  fun `should complete on subsequent parameters`() {
    myFixture.configureByText("MODULE.bazel", "module(name = 'testName', <caret>)")
    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings()

    val expectedArgNames =
      BazelGlobalFunctions.moduleGlobalFunctions(myFixture.project)["module"]!!
        .params
        .filter { it.name.contains('a') && it.name != "name" }
        .map { it.name }
    lookups shouldContainAll expectedArgNames
  }

  @Test
  fun `should complete parameters added in Bazel 9_2`() {
    myFixture.testBazel.sync { release(BazelRelease(9, 2)) }
    myFixture.configureByText("extensions.bzl", "my_extension = module_extension(<caret>)")
    myFixture.type("facts")

    // The only match is inserted without a lookup.
    myFixture.completeBasic()

    myFixture.checkResult("my_extension = module_extension(facts_version = <caret>,)")
  }

  @Test
  fun `should not complete a parameter that is passed by its alias`() {
    myFixture.testBazel.sync { release(BazelRelease(9, 3)) }
    myFixture.configureByText("BUILD", "filegroup(applicable_licenses = [], <caret>)")
    myFixture.type("package_meta")

    myFixture.completeBasic()

    myFixture.checkResult("filegroup(applicable_licenses = [], package_meta<caret>)")
  }

  @Test
  fun `should complete with empty quotes`() {
    myFixture.configureByText("MODULE.bazel", "module(<caret>)")
    myFixture.type("name")

    val lookupElements = myFixture.completeBasic()

    // Select first lookup element and simulate pressing Tab key to trigger insert handler.
    if (lookupElements != null && lookupElements.isNotEmpty()) {
      runInEdtAndWait { myFixture.lookup?.currentItem = lookupElements[0] }
      myFixture.type('\t')
    }

    myFixture.checkResult("""module(name = '<caret>',)""")
  }
}
