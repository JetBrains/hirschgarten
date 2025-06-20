package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BazelGlobalFunctionArgumentCompletionTest(val functionName: String, val argNames: List<String>) : BasePlatformTestCase() {
  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data(): Collection<Array<Any>> =
      BazelGlobalFunctions.MODULE_FUNCTIONS.map { (funName, funDef) ->
        arrayOf(funName, funDef.params.map { it.name })
      }
  }

  @Test
  fun `should complete arguments for global MODULE functions`() {
    // given
    myFixture.configureByText("MODULE.bazel", "$functionName(<caret>)")
    myFixture.type("a")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll argNames.filter { it.contains("a") }
  }
}
