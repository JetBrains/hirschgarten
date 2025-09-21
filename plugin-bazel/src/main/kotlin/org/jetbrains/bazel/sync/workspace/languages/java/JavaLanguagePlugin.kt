package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.util.EnvironmentUtil
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmBuildTarget
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class JavaLanguagePlugin(
  private val bazelPathsResolver: BazelPathsResolver,
  private val jdkResolver: JdkResolver,
  private val packageResolver: JvmPackageResolver,
) : LanguagePlugin<JvmBuildTarget>,
  JVMPackagePrefixResolver {
  private var jdk: Jdk? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverride
    jdk = ideJavaHomeOverride?.let { Jdk(version = "ideJavaHomeOverride", javaHome = it) } ?: jdkResolver.resolve(targets)
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): JvmBuildTarget? {
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTarget = target.jvmTargetInfo
    val binaryOutputs = jvmTarget.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)
    val mainClass = getMainClass(jvmTarget)

    val jdk = jdk ?: return null
    val javaHome = jdk.javaHome ?: return null
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }
    return JvmBuildTarget(
      javaVersion = javaVersionFromJavacOpts(jvmTarget.javacOptsList) ?: jdk.version,
      javaHome = javaHome,
      javacOpts = jvmTarget.javacOptsList,
      binaryOutputs = binaryOutputs,
      environmentVariables = environmentVariables,
      mainClass = mainClass,
      jvmArgs = jvmTarget.jvmFlagsList,
      programArgs = jvmTarget.argsList,
    )
  }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.JAVA)

  override fun resolveJvmPackagePrefix(source: Path): String? = packageResolver.calculateJvmPackagePrefix(source)

  override fun collectResources(targetInfo: TargetInfo): Sequence<Path> {
    if (!targetInfo.hasJvmTargetInfo()) return emptySequence()
    val base = targetInfo.resourcesList.asSequence().map(bazelPathsResolver::resolve)
    return base + resolveAdditionalResources(targetInfo)
  }

  override fun targetSupportsStrictDeps(target: TargetInfo): Boolean = target.hasJvmTargetInfo() && !target.hasKotlinTargetInfo() && !target.hasScalaTargetInfo()

  override fun collectJdepsLibraries(
    targetsToImport: Map<Label, TargetInfo>,
    existingPerTargetLibs: Map<Label, List<Library>>,
    allKnownLibraries: Map<Label, Library>,
    interfacesAndBinaries: Map<Label, Set<Path>>,
  ): Map<Label, List<Library>> {
    val targetsToJdepsJars = getAllJdepsDependencies(targetsToImport, existingPerTargetLibs, allKnownLibraries)
    val libraryNameToLibraryValueMap = HashMap<Label, Library>()
    return targetsToJdepsJars.mapValues { (targetLabel, jars) ->
      val excluded = interfacesAndBinaries[targetLabel].orEmpty()
      jars
        .map { bazelPathsResolver.resolve(it) }
        .filter { it !in excluded }
        .mapNotNull { jar ->
          if (shouldSkipJdepsJar(jar)) null else {
            val label = syntheticLabel(jar)
            libraryNameToLibraryValueMap.computeIfAbsent(label) {
              Library(
                label = label,
                dependencies = emptyList(),
                interfaceJars = emptySet(),
                outputs = setOf(jar),
                sources = emptySet(),
              )
            }
          }
        }
    }
  }

  private fun getAllJdepsDependencies(
    targetsToImport: Map<Label, TargetInfo>,
    libraryDependencies: Map<Label, List<Library>>,
    librariesToImport: Map<Label, Library>,
  ): Map<Label, Set<Path>> {
    val jdepsJars: Map<Label, Set<Path>> = targetsToImport.values
      .filter { targetSupportsJdeps(it) }
      .map { it.label() to dependencyJarsFromJdepsFiles(it) }
      .filter { it.second.isNotEmpty() }
      .toMap()

    val allJdepsJars: Set<Path> = jdepsJars.values.flatten().toSet()
    val cache = ConcurrentHashMap<Label, Set<Path>>()

    return jdepsJars.mapValues { (targetLabel, jarsFromJdeps) ->
      val transitive = getJdepsJarsFromTransitiveDependencies(
        targetLabel,
        targetsToImport,
        libraryDependencies,
        librariesToImport,
        cache,
        allJdepsJars,
      )
      (jarsFromJdeps - transitive).toSet()
    }.filterValues { it.isNotEmpty() }
  }

  private fun getJdepsJarsFromTransitiveDependencies(
    targetOrLibrary: Label,
    targetsToImport: Map<Label, TargetInfo>,
    libraryDependencies: Map<Label, List<Library>>,
    librariesToImport: Map<Label, Library>,
    cache: ConcurrentHashMap<Label, Set<Path>>,
    allJdepsJars: Set<Path>,
  ): Set<Path> =
    cache.getOrPut(targetOrLibrary) {
      val jarsFromTargets = targetsToImport[targetOrLibrary]?.let { getTargetOutputJarsSet(it) + getTargetInterfaceJarsSet(it) }.orEmpty()
      val jarsFromLibraries = librariesToImport[targetOrLibrary]?.let { it.outputs + it.interfaceJars }.orEmpty()
      val outputJars = (jarsFromTargets + jarsFromLibraries).filter { it in allJdepsJars }.toMutableSet()

      val dependencies =
        targetsToImport[targetOrLibrary]?.dependenciesList.orEmpty().map { it.label() } +
          libraryDependencies[targetOrLibrary].orEmpty().map { it.label } +
          librariesToImport[targetOrLibrary]?.dependencies.orEmpty()

      dependencies.flatMapTo(outputJars) { dep ->
        getJdepsJarsFromTransitiveDependencies(dep, targetsToImport, libraryDependencies, librariesToImport, cache, allJdepsJars)
      }
      outputJars
    }

  private fun dependencyJarsFromJdepsFiles(targetInfo: TargetInfo): Set<Path> =
    targetInfo.jvmTargetInfo.jdepsList
      .flatMap { jdeps ->
        val path = bazelPathsResolver.resolve(jdeps)
        if (path.toFile().exists()) {
          val dependencyList = path.toFile().inputStream().use { Deps.Dependencies.parseFrom(it).dependencyList }
          dependencyList
            .asSequence()
            .filter { it.isRelevant() }
            .map { bazelPathsResolver.resolveOutput(Paths.get(it.path)) }
            .toList()
        } else {
          emptySet()
        }
      }.toSet()

  private fun Deps.Dependency.isRelevant(): Boolean =
    this.kind == Deps.Dependency.Kind.EXPLICIT || this.kind == Deps.Dependency.Kind.IMPLICIT

  private fun targetSupportsJdeps(targetInfo: TargetInfo): Boolean = targetInfo.hasJvmTargetInfo()

  private fun getTargetOutputJarsSet(targetInfo: TargetInfo): Set<Path> =
    targetInfo.jvmTargetInfo.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve).toSet()

  private fun getTargetInterfaceJarsSet(targetInfo: TargetInfo): Set<Path> =
    targetInfo.jvmTargetInfo.jarsList.flatMap { it.interfaceJarsList }.map(bazelPathsResolver::resolve).toSet()

  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()

  private fun syntheticLabel(lib: Path): Label {
    val shaOfPath = Hashing.sha256().hashString(lib.toString(), StandardCharsets.UTF_8).toString().take(7)
    val safeName = lib.fileName.toString().replace(replacementRegex, "-")
    return Label.synthetic("${'$'}safeName-${'$'}shaOfPath")
  }

  private fun shouldSkipJdepsJar(jar: Path): Boolean {
    val name = jar.fileName.toString()
    return name.startsWith("header_") && jar.parent.resolve("processed_${'$'}{name.substring(7)}").toFile().exists()
  }

  override fun collectPerTargetLibraries(targets: Sequence<TargetInfo>): Map<Label, List<Library>> =
    targets
      .filter { it.hasJvmTargetInfo() }
      .mapNotNull { targetInfo ->
        val libs = mutableListOf<Library>()
        // Annotation processors/generated jars library
        if (targetInfo.jvmTargetInfo.generatedJarsList.isNotEmpty()) {
          libs += Library(
            label = Label.parse(targetInfo.id + "_generated"),
            outputs = targetInfo.jvmTargetInfo.generatedJarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve).toSet(),
            sources = targetInfo.jvmTargetInfo.generatedJarsList.flatMap { it.sourceJarsList }.map(bazelPathsResolver::resolve).toSet(),
            dependencies = emptyList(),
            interfaceJars = emptySet(),
          )
        }
        // Output jars library (only outputs, used e.g., for srcjars/AP-generated APIs)
        if (shouldCreateOutputJarsLibrary(targetInfo)) {
          libs += Library(
            label = Label.parse(targetInfo.id + "_output_jars"),
            outputs = targetInfo.jvmTargetInfo.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve).toSet(),
            sources = targetInfo.jvmTargetInfo.jarsList.flatMap { it.sourceJarsList }.map(bazelPathsResolver::resolve).toSet(),
            dependencies = emptyList(),
            interfaceJars = emptySet(),
            isFromInternalTarget = true,
          )
        }
        if (libs.isEmpty()) null else (targetInfo.label() to libs)
      }
      .toMap()

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo): Boolean =
    !targetInfo.kind.endsWith("_resources") && targetInfo.hasJvmTargetInfo() && (
      targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
        (targetInfo.sourcesList.isNotEmpty() && !hasKnownJvmSources(targetInfo)) ||
        targetInfo.jvmTargetInfo.hasApiGeneratingPlugins
      )

  private fun hasKnownJvmSources(targetInfo: TargetInfo): Boolean =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
    }

  private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? = jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

  private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? =
    javacOpts.firstNotNullOfOrNull {
      val flagName = it.substringBefore(' ')
      val argument = it.substringAfter(' ')
      if (flagName == "-target" || flagName == "--target" || flagName == "--release") argument else null
    }
}
