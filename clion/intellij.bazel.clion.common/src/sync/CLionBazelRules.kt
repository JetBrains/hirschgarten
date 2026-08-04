package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider
import kotlin.collections.map
import kotlin.collections.toSet

internal enum class CLionRuleTypes(
  val kindString: String,
  val ruleType: RuleType,
) {
  CC_BINARY("cc_binary", RuleType.BINARY),
  ;
}

internal class CLionBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind>
    get() = CLionRuleTypes.entries.map { TargetKind(kind = it.kindString, ruleType = it.ruleType) }.toSet()
}

internal fun TargetKind.includesCLion(): Boolean =
  languageClasses.contains(CLionLanguageClass.CPP)
