package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset

internal fun CodeInsightTestFixture.verifyTargetOfReferenceAtCaret(text: String) {
  val fixture = this

  // given
  val expectedLine = text.lineSequence().indexOfFirst { it.contains("<target>") }.takeIf { it != -1 }
  val expectedColumn =
    text
      .lineSequence()
      .map { it.replace("<caret>", "").indexOf("<target>") }
      .filter { it != -1 }
      .firstOrNull()
  fixture.configureByText("test.bzl", text.replace("<target>", ""))

  // when
  val reference = fixture.file.findReferenceAt(fixture.caretOffset)
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

internal val PsiElement.line: Int
  get() = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!.getLineNumber(textOffset)

internal val PsiElement.column: Int
  get() = textOffset - containingFile.getLineStartOffset(line, skipWhitespace = false)!!
