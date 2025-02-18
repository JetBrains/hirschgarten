package org.jetbrains.bazel.run.commandLine

fun transformProgramArguments(input: String?): List<String> = input?.split(" ") ?: emptyList()
