package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextExcludableListEntity
import java.nio.file.Path

data class DirectoriesSpec(override val values: List<Path>, override val excludedValues: List<Path>) :
  ExecutionContextExcludableListEntity<Path>()
