package com.intellij.bazel.python.backend

import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunction
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunctionMap
import org.jetbrains.bazel.sync.workspace.snapshot.mergeFileCollections
import org.jetbrains.bsp.protocol.SourceFileCollection
import kotlin.to

internal val pythonTargetMergeFunctions: MergeFunctionMap = mapOf(
  PythonBuildTarget::class to MergeFunction<PythonBuildTarget> { left, right ->
    left.copy(
      imports = (left.imports + right.imports).distinct(),
      generatedSources = mergeFileCollections(
        left = left.generatedSources ?: SourceFileCollection.EMPTY,
        right = right.generatedSources ?: SourceFileCollection.EMPTY,
      ),
      externalSources = mergeFileCollections(
        left = left.externalSources ?: SourceFileCollection.EMPTY,
        right = right.externalSources ?: SourceFileCollection.EMPTY,
      ),
    )
  },
)
