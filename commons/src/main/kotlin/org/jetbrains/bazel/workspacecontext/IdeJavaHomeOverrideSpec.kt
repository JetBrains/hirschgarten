package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import java.nio.file.Path

data class IdeJavaHomeOverrideSpec(override val value: Path?) : ExecutionContextSingletonEntity<Path?>()
