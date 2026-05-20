package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.utils.srcsExpressionMatchPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class SrcsExpressionMatchPathTest : BasePlatformTestCase() {

  @Test
  fun `string literal srcs matches exact relative path`() {
    val buildFile = configureBuildFile(
      "stringExact",
      """java_library(name = "lib", srcs = "Foo.java")""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
  }

  @Test
  fun `string literal srcs does not match a different path`() {
    val buildFile = configureBuildFile(
      "stringDifferent",
      """java_library(name = "lib", srcs = "Foo.java")""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertFalse("'Bar.java' should not match", srcsExpressionMatchPath(srcsValue, "Bar.java"))
    assertFalse("'sub/Foo.java' should not match", srcsExpressionMatchPath(srcsValue, "sub/Foo.java"))
    assertFalse("'' should not match", srcsExpressionMatchPath(srcsValue, ""))
  }

  @Test
  fun `glob srcs matches files via include pattern`() {
    val buildFile = configureBuildFile(
      "globInclude",
      """java_library(name = "lib", srcs = glob(["*.java"]))""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertTrue("'Bar.java' should match", srcsExpressionMatchPath(srcsValue, "Bar.java"))
    assertFalse("'Baz.kt' should not match", srcsExpressionMatchPath(srcsValue, "Baz.kt"))
    assertFalse("'sub/Foo.java' should not match", srcsExpressionMatchPath(srcsValue, "sub/Foo.java"))
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

    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertTrue("'pkg/Foo.java' should match", srcsExpressionMatchPath(srcsValue, "pkg/Foo.java"))
    assertTrue("'pkg/deep/Foo.java' should match", srcsExpressionMatchPath(srcsValue, "pkg/deep/Foo.java"))
    assertFalse("'excluded/Bar.java' should not match", srcsExpressionMatchPath(srcsValue, "excluded/Bar.java"))
    assertFalse("'excluded/deep/Bar.java' should not match", srcsExpressionMatchPath(srcsValue, "excluded/deep/Bar.java"))
    assertFalse("'Foo.kt' should not match", srcsExpressionMatchPath(srcsValue, "Foo.kt"))
  }

  @Test
  fun `list literal srcs matches any element exactly`() {
    val buildFile = configureBuildFile(
      "listLiteral",
      """java_library(name = "lib", srcs = ["Foo.java", "Bar.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertTrue("'Bar.java' should match", srcsExpressionMatchPath(srcsValue, "Bar.java"))
    assertFalse("'Baz.java' should not match", srcsExpressionMatchPath(srcsValue, "Baz.java"))
    assertFalse("'sub/Foo.java' should not match", srcsExpressionMatchPath(srcsValue, "sub/Foo.java"))
  }

  @Test
  fun `empty list literal srcs matches nothing`() {
    val buildFile = configureBuildFile(
      "emptyList",
      """java_library(name = "lib", srcs = [])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertFalse("'Foo.java' should not match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertFalse("'' should not match", srcsExpressionMatchPath(srcsValue, ""))
  }

  @Test
  fun `binary plus srcs matches when either operand matches`() {
    val buildFile = configureBuildFile(
      "plusLists",
      """java_library(name = "lib", srcs = ["Foo.java"] + ["Bar.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertTrue("'Bar.java' should match", srcsExpressionMatchPath(srcsValue, "Bar.java"))
    assertFalse("'Baz.java' should not match", srcsExpressionMatchPath(srcsValue, "Baz.java"))
  }

  @Test
  fun `binary plus srcs combines list and glob operands`() {
    val buildFile = configureBuildFile(
      "plusListAndGlob",
      """java_library(name = "lib", srcs = ["Hand.kt"] + glob(["*.java"]))""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'Hand.kt' should match", srcsExpressionMatchPath(srcsValue, "Hand.kt"))
    assertTrue("'Foo.java' should match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
    assertFalse("'Other.kt' should not match", srcsExpressionMatchPath(srcsValue, "Other.kt"))
    assertFalse("'Foo.py' should not match", srcsExpressionMatchPath(srcsValue, "Foo.py"))
  }

  @Test
  fun `chained binary plus srcs recurses across all operands`() {
    val buildFile = configureBuildFile(
      "plusChained",
      """java_library(name = "lib", srcs = ["A.java"] + ["B.java"] + ["C.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertTrue("'A.java' should match", srcsExpressionMatchPath(srcsValue, "A.java"))
    assertTrue("'B.java' should match", srcsExpressionMatchPath(srcsValue, "B.java"))
    assertTrue("'C.java' should match", srcsExpressionMatchPath(srcsValue, "C.java"))
    assertFalse("'D.java' should not match", srcsExpressionMatchPath(srcsValue, "D.java"))
  }

  @Test
  fun `binary minus srcs is not treated as concatenation`() {
    val buildFile = configureBuildFile(
      "minus",
      """java_library(name = "lib", srcs = ["Foo.java"] - ["Foo.java"])""",
    )

    val srcsValue = buildFile.findSrcsExpression()

    assertFalse("'Foo.java' should not match", srcsExpressionMatchPath(srcsValue, "Foo.java"))
  }

  private fun configureBuildFile(directory: String, content: String): PsiFile {
    val buildFile = myFixture.addFileToProject("$directory/BUILD", content)
    myFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
    return buildFile
  }

  private fun PsiFile.findSrcsExpression(): PsiElement {
    val ruleExpr: StarlarkCallExpression = (this as StarlarkFile).getTargetRules().single()
    return ruleExpr.getArgumentList()!!.getSrcsArgument()!!.getValue()!!
  }
}
