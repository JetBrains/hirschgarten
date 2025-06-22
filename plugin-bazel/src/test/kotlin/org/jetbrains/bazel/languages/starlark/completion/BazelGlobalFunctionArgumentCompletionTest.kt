package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BazelGlobalFunctionArgumentCompletionTest(val function: BazelGlobalFunction) : BasePlatformTestCase() {
  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data(): Array<BazelGlobalFunction> = BazelGlobalFunctions.MODULE_FUNCTIONS.values.toTypedArray()
  }

  @Test
  fun `should complete arguments for global MODULE functions`() {
    // given
    myFixture.configureByText("MODULE.bazel", "${function.name}(<caret>)")
    myFixture.type("a")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    val argNames = function.params.map { it.name }
    lookups shouldContainAll argNames.filter { it.contains("a") }
  }

  @Test
  fun `should use insert handler`() {
    val firstParam = function.params.filter { !it.positional }.getOrNull(0) ?: return

    // given
    myFixture.configureByText("MODULE.bazel", "${function.name}(<caret>)")
    myFixture.type(firstParam.name)

    // when
    val lookupElements = myFixture.completeBasic()

    // Select first lookup element and simulate pressing Tab key to trigger insert handler.
    if (lookupElements != null && lookupElements.isNotEmpty()) {
      myFixture.lookup?.currentItem = lookupElements[0]
      myFixture.type('\t')
    }

    // then
    val argName = firstParam.name
    val defaultValue = firstParam.default

    myFixture.checkResult("""${function.name}($argName = $defaultValue,)""")
  }
}
