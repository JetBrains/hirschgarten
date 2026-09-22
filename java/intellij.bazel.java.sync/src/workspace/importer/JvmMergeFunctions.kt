package org.jetbrains.bazel.workspace.importer

import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunction
import org.jetbrains.bazel.sync.workspace.snapshot.mergeLocationCollections

internal val jvmTargetMergeFunctions = mapOf(
  JvmBuildTarget::class to MergeFunction<JvmBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      binaryOutputs = mergeLocationCollections(left.binaryOutputs, right.binaryOutputs),
      outputInterfaceJars = mergeLocationCollections(left.outputInterfaceJars, right.outputInterfaceJars),
      outputSourceJars = mergeLocationCollections(left.outputSourceJars, right.outputSourceJars),
      generatedJars = (left.generatedJars + right.generatedJars).distinct(),
      jdepsJars = (left.jdepsJars + right.jdepsJars).distinct(),
      intellijPluginJars = mergeLocationCollections(left.intellijPluginJars, right.intellijPluginJars),
      containsInternalJars = left.containsInternalJars || right.containsInternalJars,
      hasExecutableInfo = left.hasExecutableInfo || right.hasExecutableInfo,
    )
  },

  KotlinBuildTarget::class to MergeFunction<KotlinBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      associates = (left.associates + right.associates).distinct(),
      stdlibJars = mergeLocationCollections(left.stdlibJars, right.stdlibJars),
      stdlibInferredSourceJars = mergeLocationCollections(left.stdlibInferredSourceJars, right.stdlibInferredSourceJars),
      exportedCompilerPluginTargetsList =
        (left.exportedCompilerPluginTargetsList + right.exportedCompilerPluginTargetsList).distinct(),
    )
  },

  ScalaBuildTarget::class to MergeFunction<ScalaBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      sdkJars = mergeLocationCollections(left.sdkJars, right.sdkJars),
      scalatestClasspathTargets = (left.scalatestClasspathTargets + right.scalatestClasspathTargets).distinct(),
    )
  },
)
