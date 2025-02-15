package org.jetbrains.plugins.bsp.run.commandLine

fun transformProgramArguments(input: String?): List<String> = input?.split(" ") ?: emptyList()
