package org.jetbrains.bazel.cpp.sync.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.BazelCompilerSettings
import org.jetbrains.bazel.cpp.sync.ExecutionRootPathResolver
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker
import org.jetbrains.bazel.cpp.sync.compiler.CompilerWrapperProvider
import org.jetbrains.bazel.cpp.sync.xcode.XCodeCompilerSettings
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.label.label
import java.io.File

/**
 * Converts [BspTargetInfo.CToolchainInfo] to interfaces used by [com.jetbrains.cidr.lang.workspace.OCResolveConfiguration]
 * See com.google.idea.blaze.cpp.BlazeConfigurationToolchainResolver
 */
object BazelConfigurationToolchainResolver {
  private val logger: Logger = Logger.getInstance(BazelConfigurationToolchainResolver::class.java)

  /** Returns the C toolchain used by each C target  */
  fun buildToolchainLookupMap(targetMap: Map<TargetKey, BspTargetInfo.TargetInfo>): Map<TargetKey, BspTargetInfo.CToolchainInfo> {
    val toolchains: Map<TargetKey, BspTargetInfo.CToolchainInfo> =
      targetMap
        .filter { it.value.hasCToolchainInfo() }
        .map { it.key to it.value.cToolchainInfo }
        .toMap()

    val toolchainDepsTable: Map<BspTargetInfo.TargetInfo, List<TargetKey>> =
      buildToolchainDepsTable(targetMap.values.toList(), toolchains)

    return buildLookupTable(toolchainDepsTable, toolchains)
  }

  private fun buildToolchainDepsTable(
    targets: List<BspTargetInfo.TargetInfo>,
    toolchains: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
  ): Map<BspTargetInfo.TargetInfo, List<TargetKey>> {
    val toolchainDepsTable = mutableMapOf<BspTargetInfo.TargetInfo, List<TargetKey>>()
    for (target in targets) {
      if (!target.hasCppTargetInfo() || target.hasCToolchainInfo()) {
        continue
      }
      val toolchainDeps: List<TargetKey> =
        target.dependenciesList
          .map { TargetKey(it.label()) }
          .filter { toolchains.containsKey(it) }
      toolchainDepsTable.put(target, toolchainDeps)
    }
    return toolchainDepsTable
  }

  private fun buildLookupTable(
    toolchainDepsTable: Map<BspTargetInfo.TargetInfo, List<TargetKey>>,
    toolchains: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
  ): Map<TargetKey, BspTargetInfo.CToolchainInfo> {
    val lookupTable = mutableMapOf<TargetKey, BspTargetInfo.CToolchainInfo>()
    for (entry in toolchainDepsTable) {
      val target: BspTargetInfo.TargetInfo = entry.key
      val toolchainDeps: List<TargetKey> = entry.value
      if (!toolchainDeps.isEmpty()) {
        val toolchainKey = toolchainDeps.first()
        val toolchainInfo: BspTargetInfo.CToolchainInfo = toolchains[toolchainKey] ?: continue
        lookupTable.put(TargetKey(target.label()), toolchainInfo)
      } else {
        val arbitraryToolchain: BspTargetInfo.CToolchainInfo = toolchains.values.firstOrNull() ?: continue
        lookupTable.put(TargetKey(target.label()), arbitraryToolchain)
      }
    }
    return lookupTable
  }

  private fun usesAppleCcToolchain(target: BspTargetInfo.TargetInfo): Boolean =
    target.dependenciesList
      .map { it.label().toString() }
      .any { it.startsWith("//tools/osx/crosstool") }

  /**
   * Returns the compiler settings for each toolchain.
   */
  fun buildCompilerSettingsMap(
    project: Project,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
    executionRootPathResolver: ExecutionRootPathResolver,
    oldCompilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings>,
    xcodeCompilerSettings: XCodeCompilerSettings?,
  ): Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings> =
    doBuildCompilerSettingsMap(
      project,
      toolchainLookupMap,
      executionRootPathResolver,
      xcodeCompilerSettings,
      oldCompilerSettings,
    )

  private fun resolveCompilerExecutable(executionRootPathResolver: ExecutionRootPathResolver, compilerPath: ExecutionRootPath): File {
    var compilerFile: File = executionRootPathResolver.resolveExecutionRootPath(compilerPath)

    if (!compilerFile.exists() && SystemInfo.isWindows) {
      // bazel reports the compiler executable without the exe suffix
      compilerFile = File(compilerFile.absolutePath + ".exe")
    }
    return compilerFile
  }

  private fun mergeCompilerVersions(cVersion: String?, cppVersion: String?): String? {
    if (cVersion == null) {
      return cppVersion
    }
    if (cppVersion == null) {
      return cVersion
    }
    if (cVersion == cppVersion) {
      return cppVersion
    }
    logger.warn("C and Cpp compiler version mismatch. Defaulting to Cpp compiler version.")
    return cppVersion
  }

  private fun doBuildCompilerSettingsMap(
    project: Project,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
    executionRootPathResolver: ExecutionRootPathResolver,
    xcodeCompilerSettings: XCodeCompilerSettings?,
    oldCompilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings>,
  ): Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings> {
    val toolchains: Set<BspTargetInfo.CToolchainInfo> = toolchainLookupMap.values.toSet()
    val res =
      runBlocking {
        toolchains
          .map { toolchain ->
            async {
              val cCompiler: File =
                resolveCompilerExecutable(
                  executionRootPathResolver,
                  ExecutionRootPath(toolchain.cCompiler),
                )

              val cppCompiler: File =
                resolveCompilerExecutable(
                  executionRootPathResolver,
                  ExecutionRootPath(toolchain.cCompiler),
                )

              val cCompilerVersion =
                getCompilerVersion(
                  project,
                  executionRootPathResolver,
                  xcodeCompilerSettings,
                  cCompiler,
                )
              val cppCompilerVersion =
                getCompilerVersion(
                  project,
                  executionRootPathResolver,
                  xcodeCompilerSettings,
                  cppCompiler,
                )
              val compilerVersion = mergeCompilerVersions(cCompilerVersion, cppCompilerVersion) ?: return@async null
              val oldSettings: BazelCompilerSettings? = oldCompilerSettings[toolchain]
              if (oldSettings != null &&
                oldSettings.compilerVersion == compilerVersion
              ) {
                return@async toolchain to oldSettings
              }

              val settings: BazelCompilerSettings =
                createBazelCompilerSettings(
                  project,
                  toolchain,
                  xcodeCompilerSettings,
                  executionRootPathResolver.getExecutionRoot() ?: return@async null,
                  cCompiler,
                  cppCompiler,
                  compilerVersion,
                ) ?: return@async null
              return@async toolchain to settings
            }
          }.awaitAll()
          .filterNotNull()
      }.toMap()

    return res
  }

  private fun getCompilerVersion(
    project: Project,
    executionRootPathResolver: ExecutionRootPathResolver,
    xcodeCompilerSettings: XCodeCompilerSettings?,
    executable: File,
  ): String? {
    val executionRoot: File = executionRootPathResolver.getExecutionRoot() ?: return null
    val compilerEnvFlags: Map<String, String> =
      xcodeCompilerSettings?.asEnvironmentVariables() ?: emptyMap()
    return CompilerVersionChecker
      .getInstance()
      ?.checkCompilerVersion(executionRoot, executable, compilerEnvFlags)
  }

  private fun createBazelCompilerSettings(
    project: Project,
    toolchainIdeInfo: BspTargetInfo.CToolchainInfo,
    xcodeCompilerSettings: XCodeCompilerSettings?,
    executionRoot: File,
    cCompiler: File,
    cppCompiler: File,
    compilerVersion: String,
  ): BazelCompilerSettings? {
    val compilerWrapperEnvVars: Map<String, String> = xcodeCompilerSettings?.asEnvironmentVariables() ?: emptyMap()
    val cCompilerWrapper: File? =
      CompilerWrapperProvider
        .getInstance()
        ?.createCompilerExecutableWrapper(executionRoot, cCompiler, compilerWrapperEnvVars)
    if (cCompilerWrapper == null) {
      logger.error("Unable to create compiler wrapper for: $cCompiler")
      return null
    }
    val cppCompilerWrapper: File? =
      CompilerWrapperProvider
        .getInstance()
        ?.createCompilerExecutableWrapper(executionRoot, cppCompiler, compilerWrapperEnvVars)
    if (cppCompilerWrapper == null) {
      logger.error("Unable to create compiler wrapper for: $cppCompiler")
      return null
    }

    return BazelCompilerSettings(
      project,
      cCompilerWrapper.toPath(),
      cppCompilerWrapper.toPath(),
      toolchainIdeInfo.cOptionList,
      toolchainIdeInfo.cppOptionList,
      compilerVersion,
      compilerWrapperEnvVars,
      toolchainIdeInfo.builtInIncludeDirectoryList.map { ExecutionRootPath(it) },
    )
  }
}
