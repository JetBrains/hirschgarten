package org.jetbrains.bazel.ui.widgets.tool.window.components

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.index.TargetTreeEntry
import org.jetbrains.bazel.sync_new.flow.index.TargetTreeFlags
import org.jetbrains.bsp.protocol.BuildTarget

interface TargetTreeCompat {
  val label: Label
  val name: String
  val isTestable: Boolean
  val isExecutable: Boolean
  val noBuild: Boolean
}

@JvmInline
value class TargetUtilsTargetTreeCompat(
  private val target: BuildTarget
) : TargetTreeCompat {
  override val label: Label
    get() = target.id
  override val name: String
    get() = target.id.toString()
  override val isTestable: Boolean
    get() = target.kind.ruleType == RuleType.TEST
  override val isExecutable: Boolean
    get() = target.kind.ruleType == RuleType.BINARY
  override val noBuild: Boolean
    get() = target.noBuild
}

@JvmInline
value class SyncV2TargetTreeCompat(
  private val target: TargetTreeEntry
) : TargetTreeCompat {
  override val label: Label
    get() = target.label
  override val name: String
    get() = target.name
  override val isTestable: Boolean
    get() = target.flags.contains(TargetTreeFlags.TESTABLE)
  override val isExecutable: Boolean
    get() = target.flags.contains(TargetTreeFlags.RUNNABLE)
  override val noBuild: Boolean
    get() = target.flags.contains(TargetTreeFlags.NO_BUILD)
}
