package org.jetbrains.bazel.python.lang

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal class PythonBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind>
    get() = setOf(
      TargetKind("py_binary", setOf(PythonLanguageClass.PYTHON), RuleType.BINARY),
      TargetKind("py_test", setOf(PythonLanguageClass.PYTHON), RuleType.TEST),
      TargetKind("py_library", setOf(PythonLanguageClass.PYTHON), RuleType.LIBRARY),
    )
}
