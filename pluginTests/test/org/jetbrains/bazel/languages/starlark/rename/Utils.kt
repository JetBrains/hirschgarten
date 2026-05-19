package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute

fun CodeInsightTestFixture.renameBazelTarget(old: String, new: String) {
  renameElement(requireCallWithNameAttribute(old), new)
}
