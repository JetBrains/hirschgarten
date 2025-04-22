package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity

data class BuildFlagsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()
