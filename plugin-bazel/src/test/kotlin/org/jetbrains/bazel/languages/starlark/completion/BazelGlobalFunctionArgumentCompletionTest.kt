package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionsService
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelGlobalFunctionArgumentCompletionTest : BasePlatformTestCase() {
  @Test
  fun `should complete module function`() {
    // given
    myFixture.configureByText("MODULE.bazel", "module(<caret>)")
    myFixture.type("a")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    val expectedArgNames =
      service<BazelGlobalFunctionsService>()
        .getModuleFunctions()["module"]!!
        .params
        .filter { it.name.contains('a') }
        .map { it.name }
    lookups shouldContainAll expectedArgNames
  }

  @Test
  fun `should complete on subsequent parameters`() {
    // given
    myFixture.configureByText("MODULE.bazel", "module(name = 'testName', <caret>)")
    myFixture.type("a")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    val expectedArgNames =
      service<BazelGlobalFunctionsService>()
        .getModuleFunctions()["module"]!!
        .params
        .filter { it.name.contains('a') }
        .map { it.name }
    lookups shouldContainAll expectedArgNames
  }

  @Test
  fun `should complete with empty quotes`() {
    // given
    myFixture.configureByText("MODULE.bazel", "module(<caret>)")
    myFixture.type("name")

    // when
    val lookupElements = myFixture.completeBasic()

    // Select first lookup element and simulate pressing Tab key to trigger insert handler.
    if (lookupElements != null && lookupElements.isNotEmpty()) {
      myFixture.lookup?.currentItem = lookupElements[0]
      myFixture.type('\t')
    }

    // then
    myFixture.checkResult("""module(name = '<caret>',)""")
  }

  @Test
  fun `should complete with default value and selection`() {
    // given
    myFixture.configureByText("MODULE.bazel", "module(<caret>)")
    myFixture.type("compatibility_level")

    // when
    val lookupElements = myFixture.completeBasic()

    // Select first lookup element and simulate pressing Tab key to trigger insert handler.
    if (lookupElements != null && lookupElements.isNotEmpty()) {
      myFixture.lookup?.currentItem = lookupElements[0]
      myFixture.type('\t')
    }

    // then
    myFixture.checkResult("""module(compatibility_level = <selection>0<caret></selection>,)""")
  }
}
