package com.intellij.bazel.python.backend

import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunction
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunctionMap
import org.jetbrains.bazel.sync.workspace.snapshot.mergeLocationCollections

internal val pythonTargetMergeFunctions: MergeFunctionMap = mapOf(
  PythonBuildTarget::class to MergeFunction<PythonBuildTarget> { left, right ->
    left.copy(
      imports = (left.imports + right.imports).distinct(),
      generatedSources = mergeLocationCollections(left.generatedSources, right.generatedSources),
      externalSources = mergeLocationCollections(left.externalSources, right.externalSources),
    )
  },
)
