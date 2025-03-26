package org.jetbrains.bsp.protocol
import org.jetbrains.bazel.label.Label
import java.net.URI

data class BuildTarget(
  val id: Label,
  val tags: List<String>,
  val languageIds: List<String>,
  val dependencies: List<Label>,
  val capabilities: BuildTargetCapabilities,
  val sources: List<SourceItem>,
  val resources: List<String>,
  val baseDirectory: String? = null,
  var data: BuildTargetData? = null,
) {
  val displayName: String = id.toShortString()
}

sealed interface BuildTargetData

public data class KotlinBuildTarget(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  var jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

data class PythonBuildTarget(val version: String?, val interpreter: String?) : BuildTargetData

data class ScalaBuildTarget(
  val scalaOrganization: String,
  val scalaVersion: String,
  val scalaBinaryVersion: String,
  val platform: ScalaPlatform,
  val jars: List<String>,
  val jvmBuildTarget: JvmBuildTarget? = null,
) : BuildTargetData

// TODO: change to interface
data class JvmBuildTarget(val javaHome: String, val javaVersion: String) : BuildTargetData

data class GoBuildTarget(
  val sdkHomePath: URI?,
  val importPath: String,
  val generatedLibraries: List<URI>,
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
  val androidJar: String,
  val androidTargetType: AndroidTargetType,
  val manifest: String?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<String>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<String>,
  val apk: String? = null,
  var jvmBuildTarget: JvmBuildTarget? = null,
  var kotlinBuildTarget: KotlinBuildTarget? = null,
) : BuildTargetData
