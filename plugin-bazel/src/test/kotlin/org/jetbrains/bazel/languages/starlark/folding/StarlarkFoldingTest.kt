package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class StarlarkFoldingTest(private val code: String, private val expectedPlaceholders: List<String>) : BasePlatformTestCase() {
  @Test
  fun `test folding`() {
    lateinit var file: PsiFile
    WriteCommandAction.runWriteCommandAction(project) {
      file = myFixture.configureByText("example.bzl", code)
    }
    myFixture.doHighlighting()
    val foldingDescriptors = collectFoldingDescriptors(file)
    foldingDescriptors.map { it.placeholderText } shouldContainExactly expectedPlaceholders
  }

  private fun collectFoldingDescriptors(file: PsiFile): Array<FoldingDescriptor> {
    val foldingBuilder = StarlarkFoldingBuilder()
    return foldingBuilder.buildFoldRegions(file, file.viewProvider.document!!, quick = true)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: Test with code={0}, expectedPlaceholders={1}")
    fun data(): Collection<Array<Any>> =
      listOf(
        // parentheses
        arrayOf(
          """
          (
              some_variable + another_variable
          )
          """.trimIndent(),
          listOf("(...)"),
        ),
        // array
        arrayOf(
          """
          [
              1, 2, 3, 
              4, 5
          ]
          """.trimIndent(),
          listOf("[...]"),
        ),
        // function (rule) with a `name` argument
        arrayOf(
          """
          some_function(
              name = "target", 
              arg1,
              arg2,
          )
          """.trimIndent(),
          listOf("some_function(target)"),
        ),
        // function without a `name` argument
        arrayOf(
          """
          some_function(
              not_name = target, 
              arg1,
              arg2,
          )
          """.trimIndent(),
          listOf("some_function()"),
        ),
        // nested
        arrayOf(
          """
          (
            function(
              blah
            )
          )
          """.trimIndent(),
          listOf("(...)", "function()"),
        ),
      )
  }
}
