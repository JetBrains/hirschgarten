package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.utils.srcsExpressionMatchPath
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class SrcsExpressionMatchPathTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `string literal srcs matches exact relative path`() {
    val buildFile = configureBuildFile(
      "stringExact",
      """java_library(name = "lib", srcs = "Foo.java")""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Foo.java"
  }

  @Test
  fun `string literal srcs does not match a different path`() {
    val buildFile = configureBuildFile(
      "stringDifferent",
      """java_library(name = "lib", srcs = "Foo.java")""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldNotMatch "Bar.java"
    srcsValue shouldNotMatch "sub/Foo.java"
    srcsValue shouldNotMatch ""
  }

  @Test
  fun `glob srcs matches files via include pattern`() {
    val buildFile = configureBuildFile(
      "globInclude",
      """java_library(name = "lib", srcs = glob(["*.java"]))""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Foo.java"
    srcsValue shouldMatch "Bar.java"
    srcsValue shouldNotMatch "Baz.kt"
    srcsValue shouldNotMatch "sub/Foo.java"
  }

  @Test
  fun `glob srcs honours recursive include and exclude patterns`() {
    val buildFile = configureBuildFile(
      "globExclude",
      """
      |java_library(
      |    name = "lib",
      |    srcs = glob(["**/*.java"], exclude = ["excluded/**"]),
      |)
      """.trimMargin(),
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Foo.java"
    srcsValue shouldMatch "pkg/Foo.java"
    srcsValue shouldMatch "pkg/deep/Foo.java"
    srcsValue shouldNotMatch "excluded/Bar.java"
    srcsValue shouldNotMatch "excluded/deep/Bar.java"
    srcsValue shouldNotMatch "Foo.kt"
  }

  @Test
  fun `list literal srcs matches any element exactly`() {
    val buildFile = configureBuildFile(
      "listLiteral",
      """java_library(name = "lib", srcs = ["Foo.java", "Bar.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Foo.java"
    srcsValue shouldMatch "Bar.java"
    srcsValue shouldNotMatch "Baz.java"
    srcsValue shouldNotMatch "sub/Foo.java"
  }

  @Test
  fun `empty list literal srcs matches nothing`() {
    val buildFile = configureBuildFile(
      "emptyList",
      """java_library(name = "lib", srcs = [])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldNotMatch "Foo.java"
    srcsValue shouldNotMatch ""
  }

  @Test
  fun `binary plus srcs matches when either operand matches`() {
    val buildFile = configureBuildFile(
      "plusLists",
      """java_library(name = "lib", srcs = ["Foo.java"] + ["Bar.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Foo.java"
    srcsValue shouldMatch "Bar.java"
    srcsValue shouldNotMatch "Baz.java"
  }

  @Test
  fun `binary plus srcs combines list and glob operands`() {
    val buildFile = configureBuildFile(
      "plusListAndGlob",
      """java_library(name = "lib", srcs = ["Hand.kt"] + glob(["*.java"]))""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "Hand.kt"
    srcsValue shouldMatch "Foo.java"
    srcsValue shouldNotMatch "Other.kt"
    srcsValue shouldNotMatch "Foo.py"
  }

  @Test
  fun `chained binary plus srcs recurses across all operands`() {
    val buildFile = configureBuildFile(
      "plusChained",
      """java_library(name = "lib", srcs = ["A.java"] + ["B.java"] + ["C.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldMatch "A.java"
    srcsValue shouldMatch "B.java"
    srcsValue shouldMatch "C.java"
    srcsValue shouldNotMatch "D.java"
  }

  @Test
  fun `binary minus srcs is not treated as concatenation`() {
    val buildFile = configureBuildFile(
      "minus",
      """java_library(name = "lib", srcs = ["Foo.java"] - ["Foo.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    srcsValue shouldNotMatch "Foo.java"
  }

  private fun configureBuildFile(directory: String, content: String): PsiFile {
    val buildFile = myFixture.addFileToProject("$directory/BUILD", content)
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    return buildFile
  }

  private fun PsiFile.findSrcsExpression(): PsiElement =
    runReadActionBlocking {
      val ruleExpr: StarlarkCallExpression = (this as StarlarkFile).getTargetRules().single()
      ruleExpr.getArgumentList()!!.getSrcsArgument()!!.getValue()!!
    }

  private infix fun PsiElement.shouldMatch(path: String) {
    withClue("'$path' should match") {
      runReadActionBlocking { srcsExpressionMatchPath(this, path) } shouldBe true
    }
  }

  private infix fun PsiElement.shouldNotMatch(path: String) {
    withClue("'$path' should not match") {
      runReadActionBlocking { srcsExpressionMatchPath(this, path) } shouldBe false
    }
  }
}
