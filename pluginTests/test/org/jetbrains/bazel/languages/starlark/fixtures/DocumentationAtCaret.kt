package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.collections.shouldHaveAtMostSize

/** Returns the documentation HTML at the caret, or null when there is no documentation. Fails when there is more than one. */
fun CodeInsightTestFixture.documentationAtCaret(): String? {
  val pointers = runReadActionBlocking {
    IdeDocumentationTargetProvider.getInstance(project)
      .documentationTargets(editor, file, caretOffset)
      .map { it.createPointer() }
  }
  val documentations = pointers.mapNotNull { computeDocumentationBlocking(it)?.html }
  documentations shouldHaveAtMostSize 1
  return documentations.singleOrNull()?.trim()
}
