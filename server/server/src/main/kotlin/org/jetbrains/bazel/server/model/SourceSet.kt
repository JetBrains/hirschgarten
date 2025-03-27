package org.jetbrains.bazel.server.model

import java.nio.file.Path

data class SourceWithData(val source: Path, val jvmPackagePrefix: String? = null)

data class SourceSet(val sources: Set<SourceWithData>, val generatedSources: Set<SourceWithData>)
