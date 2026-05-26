package org.jetbrains.bazel.sync.workspace.targetKind

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.info.BspTargetInfo

internal fun BspTargetInfo.TargetInfo.inferRuleType(): RuleType = when {
  !hasExecutableInfo() -> RuleType.LIBRARY
  kind.endsWith("_test") -> RuleType.TEST
  else -> RuleType.BINARY
}

