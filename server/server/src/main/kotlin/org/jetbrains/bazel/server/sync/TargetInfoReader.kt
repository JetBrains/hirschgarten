package org.jetbrains.bazel.server.sync

import com.google.protobuf.TextFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.rawinfo.BspTargetInfo
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bazel.server.bzlmod.canonicalize
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.reader

fun BspTargetInfo.TargetInfo.toTargetInfo(repoMapping: RepoMapping): TargetInfo {
  fun BspTargetInfo.FileLocation.toFileLocation(): org.jetbrains.bazel.info.FileLocation =
    org.jetbrains.bazel.info.FileLocation(
      relativePath = relativePath,
      isSource = isSource,
      isExternal = isExternal,
      rootExecutionPathFragment = rootExecutionPathFragment,
    )

  fun BspTargetInfo.Dependency.toDependency(): org.jetbrains.bazel.info.Dependency =
    org.jetbrains.bazel.info.Dependency(
      id = Label.parse(id).canonicalize(repoMapping),
      dependencyType =
        when (dependencyType) {
          BspTargetInfo.Dependency.DependencyType.COMPILE -> org.jetbrains.bazel.info.DependencyType.COMPILE
          BspTargetInfo.Dependency.DependencyType.RUNTIME -> org.jetbrains.bazel.info.DependencyType.RUNTIME
          BspTargetInfo.Dependency.DependencyType.UNRECOGNIZED -> org.jetbrains.bazel.info.DependencyType.COMPILE // fallback
        },
    )

  fun BspTargetInfo.JvmOutputs.toJvmOutputs(): org.jetbrains.bazel.info.JvmOutputs =
    org.jetbrains.bazel.info.JvmOutputs(
      binaryJars = binaryJarsList.map { it.toFileLocation() },
      interfaceJars = interfaceJarsList.map { it.toFileLocation() },
      sourceJars = sourceJarsList.map { it.toFileLocation() },
    )

  fun BspTargetInfo.JvmTargetInfo.toJvmTargetInfo(): org.jetbrains.bazel.info.JvmTargetInfo =
    org.jetbrains.bazel.info.JvmTargetInfo(
      jars = jarsList.map { it.toJvmOutputs() },
      generatedJars = generatedJarsList.map { it.toJvmOutputs() },
      javacOpts = javacOptsList,
      jvmFlags = jvmFlagsList,
      mainClass = mainClass,
      args = argsList,
      jdeps = jdepsList.map { it.toFileLocation() },
      transitiveCompileTimeJars = transitiveCompileTimeJarsList.map { it.toFileLocation() },
    )

  fun BspTargetInfo.JavaToolchainInfo.toJavaToolchainInfo(): org.jetbrains.bazel.info.JavaToolchainInfo =
    org.jetbrains.bazel.info.JavaToolchainInfo(
      sourceVersion = sourceVersion,
      targetVersion = targetVersion,
      javaHome = javaHome.toFileLocation(),
    )

  fun BspTargetInfo.JavaRuntimeInfo.toJavaRuntimeInfo(): org.jetbrains.bazel.info.JavaRuntimeInfo =
    org.jetbrains.bazel.info.JavaRuntimeInfo(
      javaHome = javaHome.toFileLocation(),
    )

  fun BspTargetInfo.ScalaTargetInfo.toScalaTargetInfo(): org.jetbrains.bazel.info.ScalaTargetInfo =
    org.jetbrains.bazel.info.ScalaTargetInfo(
      scalacOpts = scalacOptsList,
      compilerClasspath = compilerClasspathList.map { it.toFileLocation() },
      scalatestClasspath = scalatestClasspathList.map { it.toFileLocation() },
    )

  fun BspTargetInfo.KotlincPluginOption.toKotlincPluginOption(): org.jetbrains.bazel.info.KotlincPluginOption =
    org.jetbrains.bazel.info.KotlincPluginOption(
      pluginId = pluginId,
      optionValue = optionValue,
    )

  fun BspTargetInfo.KotlincPluginInfo.toKotlincPluginInfo(): org.jetbrains.bazel.info.KotlincPluginInfo =
    org.jetbrains.bazel.info.KotlincPluginInfo(
      pluginJars = pluginJarsList.map { it.toFileLocation() },
      kotlincPluginOptions = kotlincPluginOptionsList.map { it.toKotlincPluginOption() },
    )

  fun BspTargetInfo.KotlinTargetInfo.toKotlinTargetInfo(): org.jetbrains.bazel.info.KotlinTargetInfo =
    org.jetbrains.bazel.info.KotlinTargetInfo(
      languageVersion = languageVersion,
      apiVersion = apiVersion,
      associates = associatesList.map { Label.parse(it).canonicalize(repoMapping) },
      kotlincOpts = kotlincOptsList,
      stdlibs = stdlibsList.map { it.toFileLocation() },
      kotlincPluginInfos = kotlincPluginInfosList.map { it.toKotlincPluginInfo() },
    )

  fun BspTargetInfo.PythonTargetInfo.toPythonTargetInfo(): org.jetbrains.bazel.info.PythonTargetInfo =
    org.jetbrains.bazel.info.PythonTargetInfo(
      interpreter = interpreter.toFileLocation(),
      version = version,
      imports = importsList,
      isCodeGenerator = isCodeGenerator,
      generatedSources = generatedSourcesList.map { it.toFileLocation() },
    )

  fun BspTargetInfo.GoTargetInfo.toGoTargetInfo(): org.jetbrains.bazel.info.GoTargetInfo =
    org.jetbrains.bazel.info.GoTargetInfo(
      importPath = importPath,
      sdkHomePath = sdkHomePath.toFileLocation(),
      generatedSources = generatedSourcesList.map { it.toFileLocation() },
      generatedLibraries = generatedLibrariesList.map { it.toFileLocation() },
      libraryLabels = libraryLabelsList.map { Label.parse(it).canonicalize(repoMapping) },
    )

  fun BspTargetInfo.CppTargetInfo.toCppTargetInfo(): org.jetbrains.bazel.info.CppTargetInfo =
    org.jetbrains.bazel.info.CppTargetInfo(
      copts = coptsList,
      headers = headersList.map { it.toFileLocation() },
      textualHeaders = textualHeadersList.map { it.toFileLocation() },
      transitiveIncludeDirectories = transitiveIncludeDirectoryList,
      transitiveQuoteIncludeDirectories = transitiveQuoteIncludeDirectoryList,
      transitiveDefines = transitiveDefineList,
      transitiveSystemIncludeDirectories = transitiveSystemIncludeDirectoryList,
      includePrefix = includePrefix,
      stripIncludePrefix = stripIncludePrefix,
    )

  fun BspTargetInfo.CToolchainInfo.toCToolchainInfo(): org.jetbrains.bazel.info.CToolchainInfo =
    org.jetbrains.bazel.info.CToolchainInfo(
      targetName = targetName,
      cppOptions = cppOptionList,
      cOptions = cOptionList,
      cCompiler = cCompiler,
      cppCompiler = cppCompiler,
      builtInIncludeDirectories = builtInIncludeDirectoryList,
    )

  fun BspTargetInfo.AndroidTargetInfo.toAndroidTargetInfo(): org.jetbrains.bazel.info.AndroidTargetInfo =
    org.jetbrains.bazel.info.AndroidTargetInfo(
      androidJar = androidJar.toFileLocation(),
      manifest = manifest.toFileLocation(),
      manifestOverrides = manifestOverridesMap,
      resourceDirectories = resourceDirectoriesList.map { it.toFileLocation() },
      resourceJavaPackage = resourceJavaPackage,
      assetsDirectories = assetsDirectoriesList.map { it.toFileLocation() },
      aidlBinaryJar = if (hasAidlBinaryJar()) aidlBinaryJar.toFileLocation() else null,
      aidlSourceJar = if (hasAidlSourceJar()) aidlSourceJar.toFileLocation() else null,
      apk = apk.toFileLocation(),
    )

  fun BspTargetInfo.AndroidAarImportInfo.toAndroidAarImportInfo(): org.jetbrains.bazel.info.AndroidAarImportInfo =
    org.jetbrains.bazel.info.AndroidAarImportInfo(
      manifest = manifest.toFileLocation(),
      resourceFolder = resourceFolder.toFileLocation(),
      rTxt = rTxt.toFileLocation(),
    )

  return TargetInfo(
    id = Label.parse(id).canonicalize(repoMapping),
    kind = kind,
    tags = tagsList,
    dependencies = dependenciesList.map { it.toDependency() },
    sources = sourcesList.map { it.toFileLocation() },
    generatedSources = generatedSourcesList.map { it.toFileLocation() },
    resources = resourcesList.map { it.toFileLocation() },
    env = envMap,
    envInherit = envInheritList,
    executable = executable,
    workspaceName = workspaceName,
    jvmTargetInfo = if (hasJvmTargetInfo()) jvmTargetInfo.toJvmTargetInfo() else null,
    javaToolchainInfo = if (hasJavaToolchainInfo()) javaToolchainInfo.toJavaToolchainInfo() else null,
    javaRuntimeInfo = if (hasJavaRuntimeInfo()) javaRuntimeInfo.toJavaRuntimeInfo() else null,
    scalaTargetInfo = if (hasScalaTargetInfo()) scalaTargetInfo.toScalaTargetInfo() else null,
    cppTargetInfo = if (hasCppTargetInfo()) cppTargetInfo.toCppTargetInfo() else null,
    kotlinTargetInfo = if (hasKotlinTargetInfo()) kotlinTargetInfo.toKotlinTargetInfo() else null,
    pythonTargetInfo = if (hasPythonTargetInfo()) pythonTargetInfo.toPythonTargetInfo() else null,
    androidTargetInfo = if (hasAndroidTargetInfo()) androidTargetInfo.toAndroidTargetInfo() else null,
    androidAarImportInfo = if (hasAndroidAarImportInfo()) androidAarImportInfo.toAndroidAarImportInfo() else null,
    goTargetInfo = if (hasGoTargetInfo()) goTargetInfo.toGoTargetInfo() else null,
    cToolchainInfo = if (hasCToolchainInfo()) cToolchainInfo.toCToolchainInfo() else null,
  )
}

class TargetInfoReader(private val bspClientLogger: BspClientLogger) {
  suspend fun readTargetMapFromAspectOutputs(files: Set<Path>, repoMapping: RepoMapping): Map<CanonicalLabel, TargetInfo> =
    withContext(Dispatchers.Default) {
      files.map { file -> async { readFromFile(file) } }.awaitAll()
    }.asSequence()
      .filterNotNull()
      .groupBy { it.id }
      // If any aspect has already been run on the build graph, it created shadow graph
      // containing new nodes of the same labels as the original ones. In particular,
      // this happens for all protobuf targets, for which a built-in aspect "bazel_java_proto_aspect"
      // is run. In order to correctly address this issue, we would have to provide separate
      // entities (TargetInfos) for each target and each ruleset (or language) instead of just
      // entity-per-label. As long as we don't have it, in case of a conflict we just take the entity
      // that contains JvmTargetInfo as currently it's the most important one for us. Later, we sort by
      // shortest size to get a stable result, which should be the default config.
      .mapValues {
        it.value.filter(BspTargetInfo.TargetInfo::hasJvmTargetInfo).minByOrNull { targetInfo -> targetInfo.serializedSize }
          ?: it.value.first()
      }.mapValues { it.value.toTargetInfo(repoMapping) }
      .mapKeys { Label.parse(it.key).canonicalize(repoMapping) }

  private fun readFromFile(file: Path): BspTargetInfo.TargetInfo? {
    val builder = BspTargetInfo.TargetInfo.newBuilder()
    val parser =
      TextFormat.Parser
        .newBuilder()
        .setAllowUnknownFields(true)
        .build()
    try {
      file.reader().use {
        parser.merge(it, builder)
      }
    } catch (e: IOException) {
      // Can happen if one output path is a prefix of another, then Bazel can't create both
      bspClientLogger.error("[WARN] Could not read target info $file: ${e.message}")
      return null
    }
    return builder.build()
  }
}
