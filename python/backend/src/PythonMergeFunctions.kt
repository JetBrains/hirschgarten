package com.intellij.bazel.python.backend

import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunction
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunctionMap
import org.jetbrains.bazel.sync.workspace.snapshot.mergeLocationCollections
import org.jetbrains.bsp.protocol.StringsCollection

internal val pythonTargetMergeFunctions: MergeFunctionMap = mapOf(
  PythonBuildTarget::class to MergeFunction<PythonBuildTarget> { left, right ->
    left.copy(
      imports = StringsCollection.of((left.imports.asList() + right.imports.asList()).distinct()),
      generatedSources = mergeLocationCollections(left.generatedSources, right.generatedSources),
      generatedSourceOverrides = left.generatedSourceOverrides + right.generatedSourceOverrides,
      externalSources = mergeLocationCollections(left.externalSources, right.externalSources),
    )
  },
)
