package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

data class EarlyBazelSyncProject(val targets: Map<Label, BspTargetInfo.TargetInfo>, val hasError: Boolean)
