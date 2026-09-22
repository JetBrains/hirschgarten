package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.extractData

@ApiStatus.Internal
data class JvmBuildTarget(
  val javacOpts: List<String> = listOf(),
  val binaryOutputs: OutputLocationCollection = OutputLocationCollection.EMPTY,

  val environmentVariables: Map<String, String> = mapOf(),
  val mainClass: String? = null,
  val jvmArgs: List<String> = listOf(),
  val programArgs: List<String> = listOf(),
  val resolvedResourceStripPrefix: OutputLocation? = null,
  val outputInterfaceJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val outputSourceJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val generatedJars: List<JvmOutputs> = emptyList(),
  val jdepsJars: List<JdepsJar> = emptyList(),
  val intellijPluginJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
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
  val stdlibJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val stdlibInferredSourceJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val exportedCompilerPluginTargetsList: List<WorkspaceTargetKey> = emptyList(),
  val kspSourceJars: OutputLocationCollection = OutputLocationCollection.EMPTY
) : BuildTargetData

@ApiStatus.Internal
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val scalacOptions: List<String>,
  val scalatestClasspathTargets: List<Label> = emptyList(),
) : BuildTargetData

@ApiStatus.Internal
data class JvmOutputs(
  val binaryJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val interfaceJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val sourceJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
)

@ApiStatus.Internal
data class JdepsJar(
  val syntheticLabel: Label,
  val jar: OutputLocation,
)

@ApiStatus.Internal
data class JavaProviderData(
  val fullCompileJars: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val hasApiGeneratingPlugins: Boolean = false,
) : BuildTargetData

@ApiStatus.Internal
data class JavaToolchainData(
  val sourceVersion: String? = null,
  val targetVersion: String? = null,
  val javaHome: OutputLocation? = null,
  val bootClasspathJavaHome: OutputLocation? = null,
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
