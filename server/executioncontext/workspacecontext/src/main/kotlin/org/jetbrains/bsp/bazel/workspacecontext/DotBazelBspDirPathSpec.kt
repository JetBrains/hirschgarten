package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import java.nio.file.Path

data class DotBazelBspDirPathSpec(override val value: Path) : ExecutionContextSingletonEntity<Path>()
