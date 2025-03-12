package org.jetbrains.bazel.ogRun.producers

import com.google.idea.blaze.base.dependencies.TargetInfo
import com.google.idea.blaze.base.lang.buildfile.psi.FuncallExpression
import com.google.idea.blaze.base.model.primitives.Kind
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.model.primitives.RuleType

internal class BuildTarget(rule: FuncallExpression?, ruleType: RuleType?, label: Label?) {
    fun guessTargetInfo(): TargetInfo? {
        val ruleName: String? = rule.getFunctionName()
        if (ruleName == null) {
            return null
        }
        val kind: Kind? = Kind.fromRuleName(ruleName)
        return if (kind != null) TargetInfo.builder(label, kind.getKindString()).build() else null
    }

    val rule: FuncallExpression?
    val ruleType: RuleType?
    val label: Label?

    init {
        this.rule = rule
        this.ruleType = ruleType
        this.label = label
    }
}
