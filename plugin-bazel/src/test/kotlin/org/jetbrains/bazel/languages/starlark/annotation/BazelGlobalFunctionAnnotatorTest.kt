package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelGlobalFunctionAnnotatorTest : BasePlatformTestCase() {
  @Test
  fun `annotate duplicate named argument`() {
    val errorMessage = StarlarkBundle.message("annotator.duplicate.keyword.argument", "name")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      module(
        name = "name", 
        <error descr="$errorMessage">name = "name"</error>
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `annotate unresolved argument`() {
    val errorMessage = StarlarkBundle.message("annotator.named.parameter.not.found", "wrong_name")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      module(
        name = "name", 
        <error descr="$errorMessage">wrong_name = "whatever"</error>
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should not count kwargs as error`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      bazel_dep(name = "name")
      archive_override(
        module_name = "name", 
        kw_arg_1 = "whatever", 
        kw_arg_2 = "whatever",
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
      module(
        name = "name", 
        <error descr="$errorMessage">"1.2.3"</error>,
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `should error when keyword overrides positional`() {
    val errorMessage = StarlarkBundle.message("annotator.duplicate.keyword.argument", "name")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      module(
        "name",
        <error descr="$errorMessage">name = "someNewName"</error>,
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
      register_toolchains(
          "//:kotlin_toolchain",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }
}
