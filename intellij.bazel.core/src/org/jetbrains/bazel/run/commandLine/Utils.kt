package org.jetbrains.bazel.run.commandLine

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.toProgramArguments

@ApiStatus.Internal
fun transformProgramArguments(input: String?): List<String> = input?.toProgramArguments() ?: emptyList()
