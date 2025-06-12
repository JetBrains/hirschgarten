package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.label.Label

class GazelleTargetSpec(override val value: Label?) : ExecutionContextSingletonEntity<Label?>()
