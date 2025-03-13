package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity

data class SyncFlagsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()
