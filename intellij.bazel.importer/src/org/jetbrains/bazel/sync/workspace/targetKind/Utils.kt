package org.jetbrains.bazel.sync.workspace.targetKind

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.bazel.commons.RuleType

internal fun TargetIdeInfo.inferRuleType(): RuleType = when {
  !hasExecutableInfo() -> RuleType.LIBRARY
  kind.endsWith("_test") -> RuleType.TEST
  else -> RuleType.BINARY
}

