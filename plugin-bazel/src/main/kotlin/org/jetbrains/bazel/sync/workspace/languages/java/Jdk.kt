package org.jetbrains.bazel.sync.workspace.languages.java

import java.nio.file.Path

data class Jdk(val version: String, val javaHome: Path?)
