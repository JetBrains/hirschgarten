package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkScopeTest : BasePlatformTestCase() {
  override fun createTempDirTestFixture(): TempDirTestFixture? = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture()

  @Before
  fun beforeEach() {
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!
  }

  @Test
  fun `function scope is preferred to top-level scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          <target>foo = 2
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `parameters are preferred to top-level scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar(<target>foo = 2):
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `default values resolve to outer scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      <target>foo = 1
      def bar(baz = <caret>foo):
          foo = 2
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to nested assignments in module block`() {
    // Resolves to the latest assignment before the reference
    verifyTargetOfReferenceAtCaret(
      """
      if 1 != 2:
          foo = 2
      else:
          foo = 3
      <target>foo = 4
      <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to nested assignments in function block`() {
    // Resolves to the latest assignment before the reference
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          if 1 != 2:
              foo = 2
          else:
              foo = 3
          <target>foo = 4
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to deeply nested assignments in module block`() {
    // Resolves to the latest assignment before the reference
    verifyTargetOfReferenceAtCaret(
      """
      if 1 != 2:
          baz()
          if 3 != 4:
              for i in range(10):
                  foo = 2
          else:
              foo = 3
          foo = 4
      <target>foo = 5
      <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to deeply nested assignments in function block`() {
    // Resolves to the latest assignment before the reference
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          if 1 != 2:
              baz()
              if 3 != 4:
                  for i in range(10):
                      foo = 2
              else:
                  foo = 3
              foo = 4
          <target>foo = 5
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to later binding in same block`() {
    // Taken from:
    // https://github.com/bazelbuild/starlark/blob/6dd78ee3a66820a8b7571239946466cc702b209e/spec.md#name-binding-and-variables
    verifyTargetOfReferenceAtCaret(
      """
      y = "goodbye"

      def hello():
          for x in (1, 2):
              if x == 2:
                  print(<caret>y) # prints "hello"
              if x == 1:
                  <target>y = "hello"
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to conditionally assigned variable`() {
    verifyTargetOfReferenceAtCaret(
      """
      if 1:
          bar = 1
      elif 2:
          <target>baz = 1
      else 3:
          quz = 1
      <caret>baz
      """.trimIndent(),
    )
  }

  @Test
  fun `falls back to higher scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      def <target>foo():
          def bar():
              def baz():
                  print(<caret>foo)
      """.trimIndent(),
    )
  }

  @Test
  fun `falls back to load`() {
    // given
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile =
      myFixture.addFileToProject(
        "defs.bzl",
        """
        if 0:
            qux = 1
        else:
            quz = 2
        """.trimIndent(),
      )

    myFixture.configureByText(
      "test.bzl",
      """
      load("//:defs.bzl", "quz")

      def foo():
          def bar():
              def baz():
                  print(<caret>quz)
      """.trimIndent(),
    )

    // when
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val resolved = reference!!.resolve()

    // then
    resolved.shouldNotBeNull()
    resolved.containingFile shouldBe defsFile
    resolved.line shouldBe 3
    resolved.column shouldBe 4
  }

  @Test
  fun `not found`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          def bar():
              def baz():
                  print(<caret>quz)
      """.trimIndent(),
    )
  }

  @Test
  fun `does not search in inner scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          bar = 1
      <caret>bar
      """.trimIndent(),
    )
  }

  @Test
  fun `does not search in parameters of inner scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(bar = 1):
          pass
      <caret>bar
      """.trimIndent(),
    )
  }

  @Test
  fun `does consider expressions before dot`() {
    verifyTargetOfReferenceAtCaret(
      """
      <target>baz = struct(bar = 500)
      <caret>baz.bar
      """.trimIndent(),
    )
  }

  @Test
  fun `does not consider expressions after dot`() {
    verifyTargetOfReferenceAtCaret(
      """
      bar = 500
      baz.<caret>bar
      """.trimIndent(),
    )
  }

  @Test
  fun `local variable hides containing function`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          <target>foo = 5
          print(<caret>foo)
      """.trimIndent(),
    )
  }

  @Test
  fun `reference to self`() {
    verifyTargetOfReferenceAtCaret(
      """
      def <target>foo():
          print(<caret>foo)
      """.trimIndent(),
    )
  }

  @Test
  fun `forward reference in top-level scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      for i in range(10):
          if i == 9:
              print(<caret>value)
          <target>value = 1
      """.trimIndent(),
    )
  }

  @Test
  fun `forward reference in function scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          for i in range(10):
              if i == 9:
                  print(<caret>value)
              <target>value = 1
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension reference from loop body`() {
    verifyTargetOfReferenceAtCaret(
      """
      [<caret>x for <target>x, y in [(1, 2), (4, 5)] if y > 3]
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension reference from if`() {
    verifyTargetOfReferenceAtCaret(
      """
      [x for x, <target>y in [(1, 2), (4, 5)] if <caret>y > 3]
      """.trimIndent(),
    )
  }

  @Test
  fun `first for in comprehension resolves to surrounding scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      <target>x = [[1]]
      [1 for x in <caret>x for x in x]
      """.trimIndent(),
    )
  }

  @Test
  fun `second for in comprehension resolves to comprehension scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      x = [[1]]
      [1 for <target>x in x for x in <caret>x]
      """.trimIndent(),
    )
  }

  // Comprehension tests below taken from:
  // https://github.com/bazelbuild/bazel/commit/29bdfa392bf8a551d0fad5cee4e425e042194bf0#diff-fa5ed5faf1b8b7ddd01110eabbdbf2279e16d035f403231d7d8ca354ba506062R182-R186

  @Test
  fun `comprehension scope f`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(<target>x):
          x += [[j(x) for x in i(x)] + h(x) for x in f(<caret>x) if g(x)]
          return k(x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension scope g`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(x):
          x += [[j(x) for x in i(x)] + h(x) for <target>x in f(x) if g(<caret>x)]
          return k(x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension scope h`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(x):
          x += [[j(x) for x in i(x)] + h(<caret>x) for <target>x in f(x) if g(x)]
          return k(x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension scope i`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(x):
          x += [[j(x) for x in i(<caret>x)] + h(x) for <target>x in f(x) if g(x)]
          return k(x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension scope j`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(x):
          x += [[j(<caret>x) for <target>x in i(x)] + h(x) for x in f(x) if g(x)]
          return k(x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `comprehension scope k`() {
    verifyTargetOfReferenceAtCaret(
      """
      def foo(<target>x):
          x += [[j(x) for x in i(x)] + h(x) for x in f(x) if g(x)]
          return k(<caret>x)

      foo([[1]])
      """.trimIndent(),
    )
  }

  @Test
  fun `dict comprehension with multiple fors`() {
    verifyTargetOfReferenceAtCaret(
      """
      {
          <caret>k: v
          for x in [1]
          if x > 0
          for <target>k, v in [(1, 2), (3, 4)]
      }
      """.trimIndent(),
    )
  }

  @Test
  fun `reassignment in conditional resolves to latest binding`() {
    // Resolves to the latest assignment before the reference
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          runfiles = create_runfiles()
          if some_condition:
              runfiles = runfiles.merge(other)
          <target>runfiles = runfiles.merge(more)
          return <caret>runfiles
      """.trimIndent(),
    )
  }

  @Test
  fun `reassignment rhs resolves to previous binding`() {
    // RHS reference resolves to the latest binding before that point
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          <target>runfiles = create_runfiles()
          if some_condition:
              runfiles = <caret>runfiles.merge(other)
          return runfiles
      """.trimIndent(),
    )
  }

  @Test
  fun `second reassignment rhs resolves to conditional binding`() {
    // RHS resolves to the latest binding before it (inside the conditional)
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          runfiles = create_runfiles()
          if some_condition:
              <target>runfiles = runfiles.merge(other)
          runfiles = <caret>runfiles.merge(more)
          return runfiles
      """.trimIndent(),
    )
  }

  @Test
  fun `chained reassignments resolve to previous binding`() {
    // Each RHS resolves to the binding immediately before it
    verifyTargetOfReferenceAtCaret(
      """
      def foo():
          value = initial()
          <target>value = transform1(value)
          value = transform2(<caret>value)
          return value
      """.trimIndent(),
    )
  }

  private fun verifyTargetOfReferenceAtCaret(text: String) {
    // given
    val expectedLine = text.lineSequence().indexOfFirst { it.contains("<target>") }.takeIf { it != -1 }
    val expectedColumn =
      text
        .lineSequence()
        .map { it.replace("<caret>", "").indexOf("<target>") }
        .filter { it != -1 }
        .firstOrNull()
    myFixture.configureByText("test.bzl", text.replace("<target>", ""))

    // when
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    if (expectedLine == null && reference == null) {
      // If we expect no target, we may not even have a reference.
      return
    }
    val resolved = reference!!.resolve()

    // then
    if (expectedLine != null) {
      check(expectedColumn != null)
      resolved.shouldNotBeNull()
      resolved shouldBe instanceOf<PsiElement>()
      resolved.line shouldBe expectedLine
      resolved.column shouldBe expectedColumn
    } else {
      resolved.shouldBeNull()
    }
  }

  val PsiElement.line: Int
    get() = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!.getLineNumber(textOffset)

  val PsiElement.column: Int
    get() = textOffset - containingFile.getLineStartOffset(line, skipWhitespace = false)!!
}
