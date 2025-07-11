package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity

class ImportIjarsSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()
