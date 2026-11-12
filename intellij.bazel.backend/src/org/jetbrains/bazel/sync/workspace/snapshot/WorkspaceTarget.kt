package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
fun BuildTarget.isTestTarget(): Boolean = isTestOnly || kind.ruleType == RuleType.TEST
