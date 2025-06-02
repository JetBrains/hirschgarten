package org.jetbrains.bazel.run2.producers

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.sync.targetKind.fromRuleName

internal class BuildTarget(
  val rule: StarlarkCallExpression,
  val ruleType: RuleType,
  val label: Label,
) {
  fun guessTargetInfo(): TargetInfo? {
    val ruleName: String = rule.name ?: return null
    val kind: TargetKind? = TargetKind.fromRuleName(ruleName)
    return if (kind != null) {
      val builder = TargetInfo.newBuilder()
      builder.id = label.toString()
      builder.kind = kind.kindString
      builder.build()
    } else {
      null
    }
  }
}
