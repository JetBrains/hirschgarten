package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.readAction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkAssignmentArityMismatchInspectionTest {
  private val myFixture by bazelCodeInsightFixture()

  private val twoTargetsZeroValues = StarlarkBundle.message("inspection.description.assignment.arity.mismatch", 2, 0)
  private val twoTargetsOneValue = StarlarkBundle.message("inspection.description.assignment.arity.mismatch", 2, 1)
  private val twoTargetsThreeValues = StarlarkBundle.message("inspection.description.assignment.arity.mismatch", 2, 3)

  @BeforeEach
  fun beforeEach() {
    myFixture.enableInspections(StarlarkAssignmentArityMismatchInspection())
  }

  @Test
  fun `list assignment with too few values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsOneValue">[a, b]</error> = [1]
      <error descr="$twoTargetsOneValue">[a, b]</error> = 1
      <error descr="$twoTargetsOneValue">[a, b]</error> = (1)
      <error descr="$twoTargetsZeroValues">[a, b]</error> = {}
      <error descr="$twoTargetsOneValue">[a, b]</error> = {1: [10, 20]}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `list assignment with too many values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsThreeValues">[a, b]</error> = [1, 2, 3]
      <error descr="$twoTargetsThreeValues">[a, b]</error> = 1, 2, 3
      <error descr="$twoTargetsThreeValues">[a, b]</error> = (1, 2, 3)
      <error descr="$twoTargetsThreeValues">[a, b]</error> = {1: 10, 2: 20, 3: 30}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple assignment with too few values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsOneValue">a, b</error> = [1]
      <error descr="$twoTargetsOneValue">a, b</error> = 1
      <error descr="$twoTargetsOneValue">a, b</error> = (1)
      <error descr="$twoTargetsZeroValues">a, b</error> = {}
      <error descr="$twoTargetsOneValue">a, b</error> = {1: [10, 20]}
      <error descr="$twoTargetsOneValue">a, b</error> = {key: 10}
      <error descr="$twoTargetsOneValue">a, b</error> = ({(1, 2): 10})
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple assignment with too many values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsThreeValues">a, b</error> = [1, 2, 3]
      <error descr="$twoTargetsThreeValues">a, b</error> = 1, 2, 3
      <error descr="$twoTargetsThreeValues">a, b</error> = (1, 2, 3)
      <error descr="$twoTargetsThreeValues">a, b</error> = {1: 10, 2: 20, 3: 30}
      <error descr="$twoTargetsThreeValues">a, b</error> = {key1: 10, key2: 20, key3: 30}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `parenthesized tuple assignment with too few values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsOneValue">(a, b)</error> = [1]
      <error descr="$twoTargetsOneValue">(a, b)</error> = 1
      <error descr="$twoTargetsOneValue">(a, b)</error> = (1)
      <error descr="$twoTargetsZeroValues">(a, b)</error> = {}
      <error descr="$twoTargetsOneValue">(a, b)</error> = {1: 10}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `parenthesized tuple assignment with too many values should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsThreeValues">(a, b)</error> = [1, 2, 3]
      <error descr="$twoTargetsThreeValues">(a, b)</error> = 1, 2, 3
      <error descr="$twoTargetsThreeValues">(a, b)</error> = (1, 2, 3)
      <error descr="$twoTargetsThreeValues">(a, b)</error> = {1: 10, 2: 20, 3: 30}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested assignment with mismatched inner arity should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a, <error descr="$twoTargetsOneValue">(b, c)</error> = [1, [2]]
      <error descr="$twoTargetsThreeValues">(d, e)</error>, f = [[1, 2, 3], 4]
      <error descr="$twoTargetsThreeValues">(g, h)</error>, <error descr="$twoTargetsOneValue">(i, j)</error> = [[1, 2, 3], [4]]
      k, <error descr="$twoTargetsOneValue">(m, n)</error> = {1: 10, (2,): 20}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `for statement with mismatched arity should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      for <error descr="$twoTargetsOneValue">a, b</error> in [[1]]: pass
      for <error descr="$twoTargetsThreeValues">(c, d)</error> in [[1, 2, 3]]: pass
      for <error descr="$twoTargetsOneValue">[e, f]</error> in [[1]]: pass
      for <error descr="$twoTargetsOneValue">g, h</error> in {(1,): [10, 20]}: pass
      for <error descr="$twoTargetsThreeValues">i, j</error> in {(1, 2, 3): [10, 20]}: pass
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `for statement with nested mismatched arity should highlight all nested mismatches`() {
    myFixture.configureByText(
      "test.bzl",
      """
      for <error descr="$twoTargetsThreeValues">(a, b)</error>, <error descr="$twoTargetsOneValue">(c, d)</error> in [[[1, 2, 3], [4]]]: pass
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `list comprehension with mismatched arity should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      result1 = [x for <error descr="$twoTargetsOneValue">a, b</error> in [[1]]]
      result2 = [x for <error descr="$twoTargetsThreeValues">(c, d)</error> in [[1, 2, 3]]]
      result3 = [x for <error descr="$twoTargetsOneValue">[e, f]</error> in [[1]]]
      result4 = [
          x
          for <error descr="$twoTargetsOneValue">a, b</error> in [[1]]
          for <error descr="$twoTargetsThreeValues">c, d</error> in [[1, 2, 3]]
      ]
      result5 = [a for <error descr="$twoTargetsOneValue">a, b</error> in {(1,): [10, 20]}]
      result6 = [a for <error descr="$twoTargetsOneValue">a, b</error> in {1: (10, 20)}]
      result7 = [a for <error descr="$twoTargetsThreeValues">a, b</error> in ({(1, 2, 3): 10})]
      result8 = [a for <error descr="$twoTargetsOneValue">a, b</error> in {(x,): 10, (y, z): 20}]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dict comprehension with mismatched arity should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      result1 = {x: x for <error descr="$twoTargetsOneValue">a, b</error> in [[1]]}
      result2 = {x: x for <error descr="$twoTargetsThreeValues">(c, d)</error> in [[1, 2, 3]]}
      result3 = {x: x for <error descr="$twoTargetsOneValue">[e, f]</error> in [[1]]}
      result4 = {a: b for <error descr="$twoTargetsThreeValues">a, b</error> in {(1, 2, 3): [10, 20]}}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `comprehension with nested mismatched arity should highlight all nested mismatches`() {
    myFixture.configureByText(
      "test.bzl",
      """
      result1 = [x for <error descr="$twoTargetsThreeValues">(a, b)</error>, <error descr="$twoTargetsOneValue">(c, d)</error> in [[[1, 2, 3], [4]]]]
      result2 = {x: x for <error descr="$twoTargetsThreeValues">(e, f)</error>, <error descr="$twoTargetsOneValue">(g, h)</error> in [[[1, 2, 3], [4]]]}
      result3 = [x for <error descr="$twoTargetsOneValue">(i, j)</error>, k in {((1,), 2): 10}]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `single literal right hand side should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$twoTargetsOneValue">c, d</error> = "value"
      <error descr="$twoTargetsOneValue">e, f</error> = True
      <error descr="$twoTargetsOneValue">g, h</error> = None
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `literal assignment targets should not crash`(): Unit = timeoutRunBlocking {
    val file = myFixture.configureByText(
      "test.bzl",
      """
      1 = 2
      (1) = 2
      [a, 1] = [1, 2]
      (a, (1)) = (1, 2)
      [1, a] = [2, 3]
      {1: 2} = {1: 2}
      """.trimIndent(),
    )
    readAction {
      val holder = ProblemsHolder(InspectionManager.getInstance(myFixture.project), file, false)
      val visitor = StarlarkAssignmentArityMismatchInspection().buildVisitor(holder, false)
      val assignments = PsiTreeUtil.findChildrenOfType(file, StarlarkAssignmentStatement::class.java)
      assertEquals(6, assignments.size)
      for (assignment in assignments) {
        assignment.accept(visitor)
      }
      assertTrue(holder.results.isEmpty())
    }
  }

  @Test
  fun `matching assignments should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a1, b1 = [1, 2]
      c1, d1 = 1, 2
      e1, f1 = (1, 2)
      g1, h1 = {1: 10, 2: 20}

      (a2, b2) = [1, 2]
      (c2, d2) = 1, 2
      (e2, f2) = (1, 2)
      (g2, h2) = ({1: 10, 2: 20})

      [a3, b3] = [1, 2]
      [c3, d3] = 1, 2
      [e3, f3] = (1, 2)
      [g3, h3] = {1: 10, 2: 20}

      for a4, b4 in [[1, 2]]: pass
      for (a5, b5) in [[1, 2]]: pass
      for [a6, b6] in [[1, 2]]: pass
      for a7, b7 in {(1, 2): 10}: pass
      for a8, b8 in ({(1, 2): 10}): pass
      for a9, b9 in {}: pass

      result1 = [x for a, b in [[1, 2]]]
      result2 = [x for a, b in {(1, 2): 10}]
      result3 = [x for (a, b) in [[1, 2]]]
      result4 = {x: x for [a, b] in [[1, 2]]}
      result5 = [
          x
          for a, b in [[1, 2]]
          for c, d in [[3, 4]]
      ]
      result6 = {a: b for a, b in {(1, 2): 10}}
      result7 = [a for a, b in ({(1, 2): 10})]
      result8 = [a for a, b in {(1, 2): 10, (3, 4): 20}]
      result9 = [a for a, b in {(x, y): 10, (z, w): 20}]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dynamic right hand side should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a, b = make_values()
      c, d = obj.field
      e, f = values[0]
      g, h = values[1:3]
      
      for i, j in make_values(): pass
      
      result1 = [x for e, f in make_values()]
      result2 = {x: x for g, h in make_values()}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
