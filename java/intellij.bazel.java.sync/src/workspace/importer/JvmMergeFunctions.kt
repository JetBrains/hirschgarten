package org.jetbrains.bazel.workspace.importer

import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.MergeFunction
import org.jetbrains.bazel.sync.workspace.snapshot.mergeFileCollections

internal val jvmTargetMergeFunctions = mapOf(
  JvmBuildTarget::class to MergeFunction<JvmBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      binaryOutputs = mergeFileCollections(left.binaryOutputs, right.binaryOutputs),
      rawBinaryOutputs = mergeFileCollections(left.rawBinaryOutputs, right.rawBinaryOutputs),
      outputInterfaceJars = mergeFileCollections(left.outputInterfaceJars, right.outputInterfaceJars),
      outputSourceJars = mergeFileCollections(left.outputSourceJars, right.outputSourceJars),
      generatedJars = (left.generatedJars + right.generatedJars).distinct(),
      jdepsJars = (left.jdepsJars + right.jdepsJars).distinct(),
      intellijPluginJars = mergeFileCollections(left.intellijPluginJars, right.intellijPluginJars),
      containsInternalJars = left.containsInternalJars || right.containsInternalJars,
      hasExecutableInfo = left.hasExecutableInfo || right.hasExecutableInfo,
    )
  },

  KotlinBuildTarget::class to MergeFunction<KotlinBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      associates = (left.associates + right.associates).distinct(),
      stdlibHardLinkedJars = mergeFileCollections(left.stdlibHardLinkedJars, right.stdlibHardLinkedJars),
      stdlibInferredSourceJars = mergeFileCollections(left.stdlibInferredSourceJars, right.stdlibInferredSourceJars),
      exportedCompilerPluginTargetsList =
        (left.exportedCompilerPluginTargetsList + right.exportedCompilerPluginTargetsList).distinct(),
    )
  },

  ScalaBuildTarget::class to MergeFunction<ScalaBuildTarget> { left, right ->
    return@MergeFunction left.copy(
      sdkJars = mergeFileCollections(left.sdkJars, right.sdkJars),
      scalatestClasspathTargets = (left.scalatestClasspathTargets + right.scalatestClasspathTargets).distinct(),
    )
  },
)
