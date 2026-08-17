package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ApiStatus.Internal
data class JvmBuildTarget(
  val javacOpts: List<String> = listOf(),
  val binaryOutputs: SourceFileCollection = SourceFileCollection.EMPTY,

  // not hard-linked outputs, needed for hotswap, bytecode viewer, etc.
  val rawBinaryOutputs: SourceFileCollection = SourceFileCollection.EMPTY,

  val environmentVariables: Map<String, String> = mapOf(),
  val mainClass: String? = null,
  val jvmArgs: List<String> = listOf(),
  val programArgs: List<String> = listOf(),
  val resolvedResourceStripPrefix: Path? = null,
  val outputInterfaceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val outputSourceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val generatedJars: List<JvmOutputs> = emptyList(),
  val jdepsJars: List<JdepsJar> = emptyList(),
  val intellijPluginJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val containsInternalJars: Boolean = false,
  val hasExecutableInfo: Boolean = false,
  val checkStrictDependencies: StrictDependencyCheckedType = StrictDependencyCheckedType.OFF,
) : BuildTargetData

@ApiStatus.Internal
data class KotlinBuildTarget(
  val languageVersion: String?,
  val apiVersion: String?,
  val kotlincOptions: List<String>,
  val associates: List<WorkspaceTargetKey>,
  val moduleName: String? = null,
  val stdlibHardLinkedJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val stdlibInferredSourceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val exportedCompilerPluginTargetsList: List<WorkspaceTargetKey> = emptyList(),
  val kspSourceJars: SourceFileCollection = SourceFileCollection.EMPTY
) : BuildTargetData

@ApiStatus.Internal
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val scalacOptions: List<String>,
  val scalatestClasspathTargets: List<Label> = emptyList(),
) : BuildTargetData

@ApiStatus.Internal
data class JvmOutputs(
  val binaryJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val interfaceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val sourceJars: SourceFileCollection = SourceFileCollection.EMPTY,
)

@ApiStatus.Internal
data class JdepsJar(
  val syntheticLabel: Label,
  val jar: Path,
)

@ApiStatus.Internal
data class JavaProviderData(
  val fullCompileJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val hasApiGeneratingPlugins: Boolean = false,
) : BuildTargetData

@ApiStatus.Internal
data class JavaToolchainData(
  val sourceVersion: String? = null,
  val targetVersion: String? = null,
  val javaHome: Path? = null,
  val bootClasspathJavaHome: Path? = null,
  val isExecConfig: Boolean = false,
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
