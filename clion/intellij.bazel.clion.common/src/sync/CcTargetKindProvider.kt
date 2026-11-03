package org.jetbrains.bazel.clion.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

@ApiStatus.Internal
val CC_TEST_KIND: TargetKind = TargetKind("cc_test", setOf(CC_LANGUAGE_CLASS), RuleType.TEST)

internal class CcTargetKindProvider : TargetKindProvider {
  override val targetKinds: Set<TargetKind> = setOf(
    TargetKind("cc_binary", setOf(CC_LANGUAGE_CLASS), RuleType.BINARY),
    TargetKind("cc_library", setOf(CC_LANGUAGE_CLASS), RuleType.LIBRARY),
    CC_TEST_KIND,
  )
}
