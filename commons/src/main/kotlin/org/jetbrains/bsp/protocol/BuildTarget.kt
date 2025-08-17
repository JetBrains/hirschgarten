package org.jetbrains.bsp.protocol
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

interface BuildTarget {
  val id: Label
  val kind: TargetKind
  val baseDirectory: Path
  val data: BuildTargetData?
  val tags: List<String>

  val noBuild: Boolean
}

data class RawBuildTarget(
  override val id: Label,
  override val tags: List<String>,
  val dependencies: List<Label>,
  override val kind: TargetKind,
  val sources: List<SourceItem>,
  val resources: List<Path>,
  override val baseDirectory: Path,
  override val noBuild: Boolean = false, // TODO https://youtrack.jetbrains.com/issue/BAZEL-1963
  override var data: BuildTargetData? = null,
  val lowPrioritySharedSources: List<SourceItem> = emptyList(),
) : BuildTarget

data class PartialBuildTarget(
  override val id: Label,
  override val tags: List<String>,
  override val kind: TargetKind,
  override val baseDirectory: Path,
  override val data: BuildTargetData? = null,
  override val noBuild: Boolean = false,
) : BuildTarget

// adding or removing new BuildTargetData should not cause cache invalidation, but still we don't want to write FQN per each target
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
// id should in 1-255 range
annotation class ClassDiscriminator(val id: Short)

sealed interface BuildTargetData

@ClassDiscriminator(1)
public data class KotlinBuildTarget(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  var jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

@ClassDiscriminator(2)
data class PythonBuildTarget(
  val version: String?,
  val interpreter: Path?,
  // imports is the attribute in bazel python rules
  // which specify a list of runfiles relative paths which will be included in PYTHONPATH
  val imports: List<String>,
  val isCodeGenerator: Boolean,
  val generatedSources: List<Path>,

  // new
  val sourceDependencies: List<Path> = listOf()
) : BuildTargetData

@ClassDiscriminator(3)
data class ScalaBuildTarget(
  val scalaVersion: String,
  val sdkJars: List<Path>,
  val jvmBuildTarget: JvmBuildTarget? = null,
  val scalacOptions: List<String>,
) : BuildTargetData

// TODO: change to interface
@ClassDiscriminator(4)
data class JvmBuildTarget(
  // not used if part of PartialBuildTarget
  @Transient @JvmField val javaHome: Path? = null,
  val javaVersion: String,

  // new
  val javacOpts: List<String> = listOf(),
  val binaryOutputs: List<Path> = listOf(),
  val environmentVariables: Map<String, String> = mapOf(),
  val mainClass: String? = null,
  val jvmArgs: List<String> = listOf(),
  val programArgs: List<String> = listOf(),
) : BuildTargetData

@ClassDiscriminator(5)
data class GoBuildTarget(
  @Transient @JvmField val sdkHomePath: Path? = null,
  val importPath: String,
  val generatedLibraries: List<Path>,
  val generatedSources: List<Path>,
  val libraryLabels: List<Label>,
) : BuildTargetData

@ClassDiscriminator(6)
data class CppBuildTarget(
  val version: String? = null,
  val compiler: String? = null,
  val cCompiler: String? = null,
  val cppCompiler: String? = null,
) : BuildTargetData

public enum class AndroidTargetType(public val value: Int) {
  APP(1),
  LIBRARY(2),
  TEST(3),
}

@ClassDiscriminator(7)
public data class AndroidBuildTarget(
  val androidJar: Path,
  val androidTargetType: AndroidTargetType,
  val manifest: Path?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<Path>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<Path>,
  val apk: Path? = null,
  var jvmBuildTarget: JvmBuildTarget? = null,
  var kotlinBuildTarget: KotlinBuildTarget? = null,
) : BuildTargetData

fun BuildTargetData.getJvmOrNull(): JvmBuildTarget? = this as? JvmBuildTarget?

@ClassDiscriminator(8)
object VoidBuildTarget : BuildTargetData
