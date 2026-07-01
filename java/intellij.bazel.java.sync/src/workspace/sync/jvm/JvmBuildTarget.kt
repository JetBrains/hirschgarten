package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ClassDiscriminator(4)
@ApiStatus.Internal
data class JvmBuildTarget(
  // not used if part of PartialBuildTarget
  @Transient @JvmField val javaHome: Path? = null,
  val javaVersion: String,
  val javacOpts: List<String> = listOf(),
  val binaryOutputs: SourceFileCollection = SourceFileCollection.EMPTY,
  val environmentVariables: Map<String, String> = mapOf(),
  val mainClass: String? = null,
  val jvmArgs: List<String> = listOf(),
  val programArgs: List<String> = listOf(),
  val resolvedResourceStripPrefix: Path? = null,
  @Transient @JvmField val libraries: List<LibraryItem> = emptyList(),
  @Transient @JvmField val jvmDependencies: List<JvmDependency> = emptyList(),
  @Transient @JvmField val outputJars: Set<Path> = emptySet(),
  val checkStrictDependencies: StrictDependencyCheckedType = StrictDependencyCheckedType.OFF,
) : BuildTargetData

@ClassDiscriminator(1)
@ApiStatus.Internal
data class KotlinBuildTarget(
  val languageVersion: String?,
  val apiVersion: String?,
  val kotlincOptions: List<String>,
  val associates: List<WorkspaceTargetKey>,
  val moduleName: String? = null,
) : BuildTargetData

@ClassDiscriminator(3)
@ApiStatus.Internal
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: List<Path>,
  val scalacOptions: List<String>,
) : BuildTargetData

@ApiStatus.Internal
sealed interface JvmDependency {
  val dependency: DependencyLabel

  data class LibraryDependency(override val dependency: DependencyLabel) : JvmDependency
  data class ModuleDependency(override val dependency: DependencyLabel) : JvmDependency
}

@ApiStatus.Internal
fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = target.extractData()

@ApiStatus.Internal
fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? = target.extractData()

