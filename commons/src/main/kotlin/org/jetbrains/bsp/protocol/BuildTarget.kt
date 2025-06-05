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

sealed interface BuildTargetData

public data class KotlinBuildTarget(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  var jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

data class PythonBuildTarget(val version: String?, val interpreter: Path?) : BuildTargetData

data class ScalaBuildTarget(
  val scalaVersion: String,
  val jars: List<Path>,
  val jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

// TODO: change to interface
data class JvmBuildTarget(val javaHome: Path, val javaVersion: String) : BuildTargetData

data class GoBuildTarget(
  val sdkHomePath: Path?,
  val importPath: String,
  val generatedLibraries: List<Path>,
) : BuildTargetData

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
