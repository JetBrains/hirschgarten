package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity

data class EnabledRulesSpec(override val values: List<String>) : ExecutionContextListEntity<String>() {
  fun isNotEmpty(): Boolean = values.isNotEmpty()
}
