package org.jetbrains.bazel.test.framework

import java.nio.file.Path

fun Path.toBazelRcPath(): String = serializeBazelRcPath(toAbsolutePath().toString())

fun serializeBazelRcPath(absolutePath: String): String =
  "'${absolutePath.replace('\\', '/').replace("'", "'\\''")}'"
