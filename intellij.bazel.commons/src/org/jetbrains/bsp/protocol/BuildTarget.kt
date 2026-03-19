package org.jetbrains.bsp.protocol
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

@ApiStatus.Internal
interface BuildTarget {
  val id: Label
  val kind: TargetKind
  val baseDirectory: Path
  val data: BuildTargetData?

  /**
   * From Bazel doc (https://bazel.build/reference/be/common-definitions)
   * manual tag will exclude the target from expansion of target pattern wildcards (..., :*, :all, etc.) and test_suite rules which do not
   * list the test explicitly when computing the set of top-level targets to build/run for the build, test, and coverage commands
   */
  val isManual: Boolean
}

@ApiStatus.Internal
data class RawBuildTarget(
  override val id: Label,
  val dependencies: List<DependencyLabel>,
  override val kind: TargetKind,
  val sources: List<SourceItem>,
  val resources: List<Path>,
  override val baseDirectory: Path,
  override var data: BuildTargetData? = null,
  val generatorName: String? = null,
  override val isManual: Boolean = false,
) : BuildTarget

@ApiStatus.Internal
data class PartialBuildTarget(
  override val id: Label,
  override val kind: TargetKind,
  override val baseDirectory: Path,
  override val data: BuildTargetData? = null,
  override val isManual: Boolean = false,
) : BuildTarget

// adding or removing new BuildTargetData should not cause cache invalidation, but still we don't want to write FQN per each target
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
// id should in 1-255 range
@ApiStatus.Internal
annotation class ClassDiscriminator(val id: Short)

@ApiStatus.Internal
sealed interface BuildTargetData

@ClassDiscriminator(1)
@ApiStatus.Internal
data class KotlinBuildTarget(
  val languageVersion: String?,
  val apiVersion: String?,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  val moduleName: String? = null,
  val jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

@ClassDiscriminator(2)
@ApiStatus.Internal
data class PythonBuildTarget(
  val version: String?,
  val interpreter: Path?,
  // imports is the attribute in bazel python rules
  // which specify a list of runfiles relative paths which will be included in PYTHONPATH
  val imports: List<String>,
  val isCodeGenerator: Boolean,
  val generatedSources: List<Path>,
  val sourceDependencies: List<Path> = listOf(),
  val mainFile: Path? = null,
  val mainModule: String? = null,
) : BuildTargetData

@ClassDiscriminator(3)
@ApiStatus.Internal
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: List<Path>,
  val jvmBuildTarget: JvmBuildTarget? = null,
  val scalacOptions: List<String>,
) : BuildTargetData

// TODO: change to interface
@ClassDiscriminator(4)
@ApiStatus.Internal
data class JvmBuildTarget(
  // not used if part of PartialBuildTarget
  @Transient @JvmField val javaHome: Path? = null,
  val javaVersion: String,
  val javacOpts: List<String> = listOf(),
  val binaryOutputs: List<Path> = listOf(),
  val environmentVariables: Map<String, String> = mapOf(),
  val mainClass: String? = null,
  val jvmArgs: List<String> = listOf(),
  val programArgs: List<String> = listOf(),
  val resourceStripPrefix: Path? = null,
) : BuildTargetData

@ClassDiscriminator(5)
@ApiStatus.Internal
data class GoBuildTarget(
  @Transient @JvmField val sdkHomePath: Path? = null,
  val importPath: String,
  val generatedLibraries: List<Path>,
  val generatedSources: List<Path>,
  val libraryLabels: List<Label>,
) : BuildTargetData

// ClassDiscriminator 6 & 7 were cpp and android, but they have been removed

@ClassDiscriminator(9)
@ApiStatus.Internal
data class ProtobufBuildTarget(
  val sources: Map<String, String>, // import path -> real file
  val jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

@ClassDiscriminator(8)
@ApiStatus.Internal
object VoidBuildTarget : BuildTargetData
