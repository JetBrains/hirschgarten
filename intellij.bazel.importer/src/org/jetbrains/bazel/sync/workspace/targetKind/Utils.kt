package org.jetbrains.bazel.sync.workspace.targetKind

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.info.BspTargetInfo

internal fun BspTargetInfo.TargetInfo.inferRuleType(): RuleType = when {
  executable && kind.endsWith("_test") -> RuleType.TEST
  executable -> RuleType.BINARY
  else -> RuleType.LIBRARY
}

