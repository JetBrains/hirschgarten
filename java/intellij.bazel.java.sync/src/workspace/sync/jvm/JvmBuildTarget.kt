package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ClassDiscriminator(4)
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
  @JvmField @Transient val outputInterfaceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val outputSourceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val generatedJars: List<JvmOutputs> = emptyList(),
  @JvmField @Transient val jdepsJars: List<JdepsJar> = emptyList(),
  @JvmField @Transient val intellijPluginJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val containsInternalJars: Boolean = false,
  @JvmField @Transient val hasExecutableInfo: Boolean = false,
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
  @JvmField @Transient val stdlibHardLinkedJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val stdlibInferredSourceJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val exportedCompilerPluginTargetsList: List<WorkspaceTargetKey> = emptyList(),
) : BuildTargetData

@ClassDiscriminator(3)
@ApiStatus.Internal
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: SourceFileCollection = SourceFileCollection.EMPTY,
  val scalacOptions: List<String>,
  @JvmField @Transient val scalatestClasspathTargets: List<Label> = emptyList(),
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

@ClassDiscriminator(10)
@ApiStatus.Internal
data class JavaProviderData(
  @JvmField @Transient val fullCompileJars: SourceFileCollection = SourceFileCollection.EMPTY,
  @JvmField @Transient val hasApiGeneratingPlugins: Boolean = false,
) : BuildTargetData

@ClassDiscriminator(11)
@ApiStatus.Internal
data class JavaToolchainData(
  @JvmField @Transient val sourceVersion: String? = null,
  @JvmField @Transient val targetVersion: String? = null,
  @JvmField @Transient val javaHome: Path? = null,
  @JvmField @Transient val bootClasspathJavaHome: Path? = null,
  @JvmField @Transient val isExecConfig: Boolean = false,
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
