package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider
import kotlin.collections.map
import kotlin.collections.toSet

private val CC_RULE_TYPES = mapOf(
  "cc_binary" to RuleType.BINARY,
  "cc_library" to RuleType.LIBRARY,
  "cc_test" to RuleType.TEST,
)

internal class CcTargetKindProvider : TargetKindProvider {

  override val targetKinds: Set<TargetKind>
    get() = CC_RULE_TYPES.entries.map { TargetKind(kind = it.key, ruleType = it.value) }.toSet()
}
