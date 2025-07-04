package org.jetbrains.bazel.info

import org.jetbrains.bazel.label.CanonicalLabel

data class FileLocation(
  val relativePath: String,
  val isSource: Boolean,
  val isExternal: Boolean,
  val rootExecutionPathFragment: String,
)

enum class DependencyType { COMPILE, RUNTIME }

data class Dependency(
  val id: CanonicalLabel,
  val dependencyType: DependencyType,
)

data class JvmOutputs(
  val binaryJars: List<FileLocation>,
  val interfaceJars: List<FileLocation>,
  val sourceJars: List<FileLocation>,
)

data class JvmTargetInfo(
  val jars: List<JvmOutputs>,
  val generatedJars: List<JvmOutputs>,
  val javacOpts: List<String>,
  val jvmFlags: List<String>,
  val mainClass: String?,
  val args: List<String>,
  val jdeps: List<FileLocation>,
  val transitiveCompileTimeJars: List<FileLocation>,
)

data class JavaToolchainInfo(
  val sourceVersion: String,
  val targetVersion: String,
  val javaHome: FileLocation,
)

data class JavaRuntimeInfo(
  val javaHome: FileLocation,
)

data class ScalaTargetInfo(
  val scalacOpts: List<String>,
  val compilerClasspath: List<FileLocation>,
  val scalatestClasspath: List<FileLocation>,
)

data class KotlincPluginOption(
  val pluginId: String,
  val optionValue: String,
)

data class KotlincPluginInfo(
  val pluginJars: List<FileLocation>,
  val kotlincPluginOptions: List<KotlincPluginOption>,
)

data class KotlinTargetInfo(
  val languageVersion: String,
  val apiVersion: String,
  val associates: List<CanonicalLabel>,
  val kotlincOpts: List<String>,
  val stdlibs: List<FileLocation>,
  val kotlincPluginInfos: List<KotlincPluginInfo>,
)

data class PythonTargetInfo(
  val interpreter: FileLocation,
  val version: String,
  val imports: List<String>,
  val isCodeGenerator: Boolean,
  val generatedSources: List<FileLocation>,
)

data class GoTargetInfo(
  val importPath: String,
  val sdkHomePath: FileLocation,
  val generatedSources: List<FileLocation>,
  val generatedLibraries: List<FileLocation>,
  val libraryLabels: List<CanonicalLabel>,
)

data class CppTargetInfo(
  val copts: List<String>,
  val headers: List<FileLocation>,
  val textualHeaders: List<FileLocation>,
  val transitiveIncludeDirectories: List<String>,
  val transitiveQuoteIncludeDirectories: List<String>,
  val transitiveDefines: List<String>,
  val transitiveSystemIncludeDirectories: List<String>,
  val includePrefix: String?,
  val stripIncludePrefix: String?,
)

data class CToolchainInfo(
  val targetName: String,
  val cppOptions: List<String>,
  val cOptions: List<String>,
  val cCompiler: String,
  val cppCompiler: String,
  val builtInIncludeDirectories: List<String>,
)

data class AndroidTargetInfo(
  val androidJar: FileLocation,
  val manifest: FileLocation,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<FileLocation>,
  val resourceJavaPackage: String,
  val assetsDirectories: List<FileLocation>,
  val aidlBinaryJar: FileLocation?,
  val aidlSourceJar: FileLocation?,
  val apk: FileLocation,
)

data class AndroidAarImportInfo(
  val manifest: FileLocation,
  val resourceFolder: FileLocation,
  val rTxt: FileLocation,
)

data class TargetInfo(
  val id: CanonicalLabel,
  val kind: String,
  val tags: List<String>,
  val dependencies: List<Dependency>,
  val sources: List<FileLocation>,
  val generatedSources: List<FileLocation>,
  val resources: List<FileLocation>,
  val env: Map<String, String>,
  val envInherit: List<String>,
  val executable: Boolean,
  val workspaceName: String,

  val jvmTargetInfo: JvmTargetInfo? = null,
  val javaToolchainInfo: JavaToolchainInfo? = null,
  val javaRuntimeInfo: JavaRuntimeInfo? = null,
  val scalaTargetInfo: ScalaTargetInfo? = null,
  val cppTargetInfo: CppTargetInfo? = null,
  val kotlinTargetInfo: KotlinTargetInfo? = null,
  val pythonTargetInfo: PythonTargetInfo? = null,
  val androidTargetInfo: AndroidTargetInfo? = null,
  val androidAarImportInfo: AndroidAarImportInfo? = null,
  val goTargetInfo: GoTargetInfo? = null,
  val cToolchainInfo: CToolchainInfo? = null,
)
