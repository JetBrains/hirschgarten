package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import com.jetbrains.cidr.lang.workspace.compiler.AppleClangSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.CachedTempFilesPool
import com.jetbrains.cidr.lang.workspace.compiler.ClangSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache.Message
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.GCCSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.MSVCSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.TempFilesPool
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.WorkspaceRoot
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionUtil
import org.jetbrains.bazel.cpp.sync.compiler.UnfilteredCompilerOptions
import org.jetbrains.bazel.cpp.sync.configuration.BazelConfigurationResolver
import org.jetbrains.bazel.cpp.sync.configuration.BazelConfigurationResolverResult
import org.jetbrains.bazel.cpp.sync.configuration.targetMap
import org.jetbrains.bazel.sync.task.bazelProject
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.io.File

// See com.google.idea.blaze.cpp.BlazeCWorkspace
// todo: this should become a service for persistence and caching
/* todo: Consider integrate this use ProjectStructureDiff. CLion doesn't use workspace model.
    Thus instead of using [AllProjectStructuresDiff], OCWorkspace.commit is directly called.
    This can cause various problems. */
class BazelCWorkspace(val project: Project) {
  val serializationVersion = 1
  var resolverResult = BazelConfigurationResolverResult.empty()

  fun update(workspaceContext: WorkspaceContext) {
    // todo: currently we always do a full sync
    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val oldResult = resolverResult
    val newResult = resolver.update(oldResult)
    // todo: add progress bar ui and metrics in our ways
    val model = calculateConfigurations(newResult)
    commit(
      serializationVersion,
      model,
      WorkspaceRoot(project.bazelProject.bazelInfo.workspaceRoot),
    )

    resolverResult = newResult
  }

  private fun commit(
    serialVersion: Int,
    workspaceModel: BazelCWorkspaceModel,
    workspaceRoot: WorkspaceRoot,
  ) {
    collectCompilerSettingsInParallel(workspaceModel, workspaceRoot)

    workspaceModel.model.setClientVersion(serialVersion)
    workspaceModel.model.preCommit()

    TransactionGuard.getInstance().submitTransactionAndWait {
      ApplicationManager
        .getApplication()
        .runWriteAction {
          workspaceModel.model.commit()
        }
    }
  }

  private fun calculateConfigurations(configResolveData: BazelConfigurationResolverResult): BazelCWorkspaceModel {
    val workspaceModifiable =
      OCWorkspaceImpl
        .getInstanceImpl(project)
        .getModifiableModel(OCWorkspace.LEGACY_CLIENT_KEY, true)
    val environmentMap = mutableMapOf<OCResolveConfiguration.ModifiableModel, CidrToolEnvironment>()
    val configurations = configResolveData.allConfigurations
    val targetMap = project.targetMap()
    val executionRootPathResolver =
      ExecutionRootPathResolver(
        project.bazelProject.bazelInfo,
        targetMap,
      )
    for (resolveConfiguration in configurations) {
      val compilerSettings = resolveConfiguration.compilerSettings
      val configLanguages = mutableMapOf<OCLanguageKind, PerLanguageCompilerOpts>()
      val configSourceFiles = mutableMapOf<VirtualFile, PerFileCompilerOpts>()

      for (targetKey in resolveConfiguration.targets) {
        val targetIdeInfo = targetMap[targetKey] ?: continue
        if (!targetIdeInfo.hasCppTargetInfo()) continue

        // defines and include directories are the same for all sources in a given target, so lets
        // collect them once and reuse for each source file's options
        val compilerSwitchesBuilder = selectSwitchBuilder(compilerSettings)

        // this parses user defined copts filed, later -I include paths are resolved using the
        // ExecutionRootPathResolver
        val coptsExtractor =
          UnfilteredCompilerOptions
            .builder()
            .registerSingleOrSplitOption("-I")
            .build(targetIdeInfo.cppTargetInfo.coptsList)

        val plainLocalCopts = coptsExtractor.uninterpretedOptions
        if (Registry.`is`("bazel.sync.workspace.filter.out.incompatible.flags")) {
          compilerSwitchesBuilder.withSwitches(filterIncompatibleFlags(plainLocalCopts))
        } else {
          compilerSwitchesBuilder.withSwitches(plainLocalCopts)
        }

        // transitiveDefines are sourced from a target's (and transitive deps) "defines" attribute
        targetIdeInfo.cppTargetInfo.transitiveDefineList.forEach { compilerSwitchesBuilder.withMacro(it) }

        val resolver = { executionRootPath: ExecutionRootPath ->
          executionRootPathResolver.resolveToIncludeDirectories(executionRootPath)
        }
        // localIncludes are sourced from -I options in a target's "copts" attribute. They can be
        // arbitrarily declared and may not exist in configResolveData.
        coptsExtractor
          .getExtractedOptionValues("-I")
          .flatMap { resolver(ExecutionRootPath(it)) }
          .map { it.absolutePath }
          .forEach {
            compilerSwitchesBuilder.withIncludePath(it)
          }

        // transitiveIncludeDirectories are sourced from CcSkylarkApiProvider.include_directories
        targetIdeInfo.cppTargetInfo.transitiveIncludeDirectoryList
          .flatMap { resolver(ExecutionRootPath(it)) }
          .filter { configResolveData.isValidHeaderRoot(it) }
          .map { it.absolutePath }
          .forEach {
            compilerSwitchesBuilder.withIncludePath(it)
          }

        // transitiveQuoteIncludeDirectories are sourced from
        // CcSkylarkApiProvider.quote_include_directories
        val quoteIncludePaths =
          targetIdeInfo.cppTargetInfo.transitiveQuoteIncludeDirectoryList
            .flatMap { resolver(ExecutionRootPath(it)) }
            .filter { configResolveData.isValidHeaderRoot(it) }
            .map { it.absolutePath }

        quoteIncludePaths.forEach {
          compilerSwitchesBuilder.withQuoteIncludePath(it)
        }
        // transitiveSystemIncludeDirectories are sourced from
        // CcSkylarkApiProvider.system_include_directories
        // Note: We would ideally use -isystem here, but it interacts badly with the switches
        // that get built by ClangUtils::addIncludeDirectories (it uses -I for system libraries).

        targetIdeInfo.cppTargetInfo.transitiveSystemIncludeDirectoryList
          .flatMap { resolver(ExecutionRootPath(it)) }
          .filter { configResolveData.isValidHeaderRoot(it) }
          .map { it.absolutePath }
          .forEach {
            compilerSwitchesBuilder.withSystemIncludePath(it)
          }

        // add builtin includes provided by the compiler as system includes
        // Note: In most cases CLion is able to derive the builtin includes during compiler info collection, unless
        // the toolchain uses an external sysroot.
        compilerSettings.builtInIncludes
          .flatMap { resolver(it) }
          .map { it.absolutePath }
          .forEach {
            compilerSwitchesBuilder.withSystemIncludePath(it)
          }

        val cCompilerSwitches =
          buildSwitchBuilder(compilerSettings, compilerSwitchesBuilder, CLanguageKind.C)
        val cppCompilerSwitches =
          buildSwitchBuilder(compilerSettings, compilerSwitchesBuilder, CLanguageKind.CPP)
        for (vf in resolveConfiguration.getSources(targetKey)) {
          val kind = resolveConfiguration.getDeclaredLanguageKind(vf)
          val perFileCompilerOpts =
            if (kind == CLanguageKind.C) {
              PerFileCompilerOpts(kind, cCompilerSwitches)
            } else {
              PerFileCompilerOpts(CLanguageKind.CPP, cppCompilerSwitches)
            }

          configSourceFiles.put(vf, perFileCompilerOpts)
          if (!configLanguages.containsKey(kind) && kind != null) {
            addConfigLanguageSwitches(
              configLanguages,
              compilerSettings,
              // If a file isn't found in configSourceFiles (newly created files), CLion uses the
              // configLanguages switches. We want some basic header search roots (genfiles),
              // which are part of every target's iquote directories. See:
              // https://github.com/bazelbuild/bazel/blob/2c493e8a2132d54f4b2fb8046f6bcef11e92cd22/src/main/java/com/google/devtools/build/lib/rules/cpp/CcCompilationHelper.java#L911
              quoteIncludePaths,
              kind,
            )
          }
        }
      }

      supportedLanguages.filter { !configLanguages.containsKey(it) }.forEach {
        addConfigLanguageSwitches(configLanguages, compilerSettings, emptyList(), it)
      }

      val id = resolveConfiguration.displayName
      val modelConfig =
        addConfiguration(
          workspaceModifiable,
          id,
          id,
          project.bazelProject.bazelInfo.workspaceRoot
            .toFile(),
          configLanguages,
          configSourceFiles,
        )
      // todo: add windows support via CppEnvironmentProvider
      // MSVC requires non-default CidrToolEnvironment
      environmentMap.put(modelConfig, CidrToolEnvironment())
    }
    return BazelCWorkspaceModel(workspaceModifiable, environmentMap)
  }

  private fun addConfigLanguageSwitches(
    configLanguages: MutableMap<OCLanguageKind, PerLanguageCompilerOpts>,
    compilerSettings: BazelCompilerSettings,
    quoteIncludePaths: List<String>,
    language: OCLanguageKind,
  ) {
    val compilerKind: OCCompilerKind = compilerSettings.getCompiler(language)
    val executable = compilerSettings.getCompilerExecutable(language) ?: return

    val switchBuilder = selectSwitchBuilder(compilerSettings)
    switchBuilder.withSwitches(compilerSettings.getCompilerSwitches(language, null))
    quoteIncludePaths.forEach(switchBuilder::withQuoteIncludePath)

    val perLanguageCompilerOpts =
      PerLanguageCompilerOpts(compilerKind, executable.toFile(), switchBuilder.build())
    configLanguages.put(language, perLanguageCompilerOpts)
  }

  // Filter out any raw copts that aren't compatible with feature detection.
  private fun filterIncompatibleFlags(copts: List<String>): List<String> =
    copts // "-include somefile.h" doesn't seem to work for some reason. E.g.,
      // "-include cstddef" results in "clang: error: no such file or directory: 'cstddef'"
      .filter { opt: String -> opt.startsWith("-include ") }

  private fun collectCompilerSettingsInParallel(workspaceModel: BazelCWorkspaceModel, workspaceRoot: WorkspaceRoot) {
    val compilerInfoCache = CompilerInfoCache()
    val tempFilesPool: TempFilesPool = CachedTempFilesPool()
    val session = compilerInfoCache.createSession<Int>(EmptyProgressIndicator())

    try {
      var i = 0
      for (config in workspaceModel.model.configurations) {
        session.schedule(
          i++,
          config,
          workspaceModel.environments[config]!!,
          workspaceRoot.directory.toString(),
        )
      }
      val messages: MultiMap<Int, Message> = MultiMap()
      session.waitForAll(messages)
      // todo: handle those messages in UI
    } catch (e: Error) {
      session.dispose() // This calls tempFilesPool.clean();
      throw e
    } catch (e: RuntimeException) {
      session.dispose()
      throw e
    }
    tempFilesPool.clean()
  }

  private data class BazelCWorkspaceModel(
    val model: OCWorkspace.ModifiableModel,
    val environments: Map<OCResolveConfiguration.ModifiableModel, CidrToolEnvironment>,
  )

  /** Group compiler options for a specific language. */
  private data class PerLanguageCompilerOpts(
    val kind: OCCompilerKind,
    val compiler: File,
    val switches: CidrCompilerSwitches,
  )

  private data class PerFileCompilerOpts(val kind: OCLanguageKind, val switches: CidrCompilerSwitches)

  companion object {
    private val logger: Logger = Logger.getInstance(BazelCWorkspace::class.java)
    private val supportedLanguages = listOf(CLanguageKind.C, CLanguageKind.CPP)

    private fun selectSwitchBuilder(compilerSettings: BazelCompilerSettings): CompilerSpecificSwitchBuilder {
      val version = compilerSettings.compilerVersion

      if (CompilerVersionUtil.isAppleClang(version)) {
        return AppleClangSwitchBuilder()
      }
      if (CompilerVersionUtil.isClang(version)) {
        return ClangSwitchBuilder()
      }
      if (CompilerVersionUtil.isMSVC(version)) {
        return MSVCSwitchBuilder()
      }

      // default to gcc
      return GCCSwitchBuilder()
    }

    private fun buildSwitchBuilder(
      compilerSettings: BazelCompilerSettings,
      builder: CompilerSpecificSwitchBuilder,
      language: OCLanguageKind,
    ): CidrCompilerSwitches =
      CidrSwitchBuilder()
        .addAllRaw(compilerSettings.getCompilerSwitches(language, null))
        .addAll(builder.build())
        .build()

    private fun addConfiguration(
      workspaceModifiable: OCWorkspace.ModifiableModel,
      id: String,
      displayName: String,
      directory: File,
      configLanguages: Map<OCLanguageKind, PerLanguageCompilerOpts>,
      configSourceFiles: Map<VirtualFile, PerFileCompilerOpts>,
    ): OCResolveConfiguration.ModifiableModel {
      val config =
        workspaceModifiable.addConfiguration(
          id,
          displayName,
          null,
          OCResolveConfiguration.DEFAULT_FILE_SEPARATORS,
        )
      for (languageEntry in configLanguages.entries) {
        val configForLanguage = languageEntry.value
        val langSettings =
          config.getLanguageCompilerSettings(languageEntry.key)
        langSettings.setCompiler(configForLanguage.kind, configForLanguage.compiler, directory)
        langSettings.setCompilerSwitches(configForLanguage.switches)
      }

      for (fileEntry in configSourceFiles.entries) {
        val compilerOpts: PerFileCompilerOpts = fileEntry.value
        val fileCompilerSettings = config.addSource(fileEntry.key, compilerOpts.kind)
        fileCompilerSettings.setCompilerSwitches(compilerOpts.switches)
      }

      return config
    }
  }
}
