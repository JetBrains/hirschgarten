package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity

data class DebugFlagsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()
