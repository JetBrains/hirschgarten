package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.assertions.throwables.shouldThrowWithMessage
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute

fun CodeInsightTestFixture.renameBazelTarget(old: String, new: String) {
  runInEdtAndWait { renameElement(requireCallWithNameAttribute(old), new) }
}

inline fun shouldThrowConflict(message: String, block: () -> Unit) {
  shouldThrowWithMessage<BaseRefactoringProcessor.ConflictsInTestsException>(message) {
    block()
  }
}
