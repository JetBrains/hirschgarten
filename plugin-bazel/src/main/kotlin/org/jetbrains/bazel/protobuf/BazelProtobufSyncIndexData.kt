package org.jetbrains.bazel.protobuf

import java.nio.file.Path

data class BazelProtobufSyncIndexData(val root: Path, val realPaths: MutableList<Path> = mutableListOf())
