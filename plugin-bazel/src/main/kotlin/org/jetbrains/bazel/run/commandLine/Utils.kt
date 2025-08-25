package org.jetbrains.bazel.run.commandLine

import org.jetbrains.bazel.commons.toProgramArguments

fun transformProgramArguments(input: String?): List<String> = input?.toProgramArguments() ?: emptyList()
