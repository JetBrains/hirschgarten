package org.jetbrains.bazel.java.ui.notifications

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.JavaLanguageClass.hasJvmSourceExtension
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaProviderData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.generatedSources
import org.jetbrains.bsp.protocol.nonGeneratedSources
import org.jetbrains.bsp.protocol.relativeNioPath
import kotlin.io.path.extension

@ApiStatus.Internal
abstract class JvmUnresolvedImportChecker : UnresolvedImportChecker() {
  override fun generatedOutputs(target: BuildTarget): Sequence<OutputLocation> {
    val sources = target.generatedSources().filter { it.isJvmSourceOrArchive() }
    val jvmBuildTarget = target.findBuildData<JvmBuildTarget>() ?: return sources
    val generatedJars = jvmBuildTarget.generatedJars.asSequence().flatMap { it.binaryJars.getOutputLocations() }
    val nonGeneratedSources = target.nonGeneratedSources().toList()
    val generatesJvmCode = nonGeneratedSources.isNotEmpty() &&
                           nonGeneratedSources.none { it.hasJvmSourceExtension() }
    val hasApiGeneratingPlugins = target.findBuildData<JavaProviderData>()?.hasApiGeneratingPlugins == true
    val outputJars =
      if (generatesJvmCode || hasApiGeneratingPlugins) {
        jvmBuildTarget.binaryOutputs.getOutputLocations()
      } else emptySequence()
    return (sources + generatedJars + outputJars).distinct()
  }
}

private val generatedSourceArchiveExtensions = setOf("srcjar", "jar")

private fun OutputLocation.isJvmSourceOrArchive(): Boolean =
  hasJvmSourceExtension() || relativeNioPath.extension in generatedSourceArchiveExtensions
