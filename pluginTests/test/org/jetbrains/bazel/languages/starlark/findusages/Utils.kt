package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.usageView.UsageInfo
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute

fun CodeInsightTestFixture.findBazelTargetUsages(name: String): Collection<UsageInfo> {
  return findUsages(requireCallWithNameAttribute(name))
}
