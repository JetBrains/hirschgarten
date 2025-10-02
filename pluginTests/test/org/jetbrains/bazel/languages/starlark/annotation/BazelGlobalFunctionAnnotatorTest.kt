package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.Environment
import org.jetbrains.bazel.languages.starlark.bazel.StarlarkGlobalFunctionProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private class TestStarlarkGlobalFunctionProvider : StarlarkGlobalFunctionProvider {
  override val functions: List<BazelGlobalFunction> =
    listOf(
      BazelGlobalFunction(
        name = "simple",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "*args",
              positional = true,
              named = false,
              required = false,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "**kwargs",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "simpleWithoutKwArgs",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "positionalNotFirst",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = false,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "optionalAndVarArgs",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = "default",
            ),
            BazelGlobalFunctionParameter(
              name = "*args",
              positional = true,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = "default",
            ),
            BazelGlobalFunctionParameter(
              name = "**kwargs",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
    )
}

@RunWith(JUnit4::class)
class BazelGlobalFunctionAnnotatorTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    ExtensionTestUtil.maskExtensions(
      StarlarkGlobalFunctionProvider.extensionPoint,
      listOf(TestStarlarkGlobalFunctionProvider()),
      myFixture.testRootDisposable,
    )
  }

  @Test
  fun `annotate duplicate named argument`() {
    val errorMessage = StarlarkBundle.message("annotator.duplicate.keyword.argument", "argTwo")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "someValue", 
        argTwo = "someOtherValue",
        <error descr="$errorMessage">argTwo = "someOtherValue"</error>
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `annotate unresolved argument`() {
    val errorMessage = StarlarkBundle.message("annotator.named.parameter.not.found", "unknownParam")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simpleWithoutKwArgs(
        "val2", "val2",
        <error descr="$errorMessage">unknownParam = "whatever"</error>
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should not count kwargs as unresolved`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "val1",
        "val2",
        kwArgOne = "val3",
        kwArgTwo = "val4",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should error positional argument after keyword argument`() {
    val errorMessage = StarlarkBundle.message("annotator.positional.argument.after.keyword.argument")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      <error descr="Missing required arguments: argOne">simple</error>(
        argTwo = "someValue",
        <error descr="$errorMessage">"someValue"</error>,
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should error when keyword overrides positional`() {
    val errorMessage = StarlarkBundle.message("annotator.duplicate.keyword.argument", "argTwo")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "val1",
        "val2",
        <error descr="$errorMessage">argTwo = "someNewName"</error>,
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should consume varargs`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "val1",
        "val2",
        "val3",
        "val4",
        "val5",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should annotate unnamed argument with a name`() {
    val errorMessage = StarlarkBundle.message("annotator.unnamed.arg.with.name", "argOne")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      <error descr="Missing required arguments: argOne">simple</error>(
        <error descr="$errorMessage">argOne = "val1"</error>,
        argTwo = "val2",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should skip positional args with default values`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      optionalAndKwArgs(
          "varArg1",
          "varArg2",
          kwArg1 = "kwArg1Value",
          kwArg2 = "kwArg2Value",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `handle positional argument not first`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      positionalNotFirst(
          "varArg2",
          argOne = "valueOne",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }
}
