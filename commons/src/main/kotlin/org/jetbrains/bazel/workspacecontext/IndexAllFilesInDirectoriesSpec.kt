package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity

class IndexAllFilesInDirectoriesSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()
