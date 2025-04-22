package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity

data class AllowManualTargetsSyncSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()
