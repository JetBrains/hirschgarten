package org.jetbrains.plugins.bsp.impl.flow.sync.languages.cpp

import com.google.common.collect.ImmutableList
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCImportGraph
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import com.jetbrains.cidr.lang.workspace.compiler.AppleClangSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.CachedTempFilesPool
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.ClangSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.GCCSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.TempFilesPool
import com.jetbrains.cidr.lang.workspace.compiler.UnknownCompilerKind
import com.jetbrains.cidr.lang.workspace.headerRoots.HeadersSearchPath
import org.jetbrains.bsp.protocol.CToolchainInfo
import org.jetbrains.bsp.protocol.CppModule
import org.jetbrains.bsp.protocol.utils.extractData
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.ToIntFunction


class CppProjectSync : ProjectSyncHook {
  private val supportedLanguages: List<OCLanguageKind> = listOf(CLanguageKind.C, CLanguageKind.CPP)
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    println("start cpp sync hook")
    val workspaceModifiable =
      OCWorkspaceImpl.getInstanceImpl(environment.project)
        .getModifiableModel(OCWorkspace.LEGACY_CLIENT_KEY, true)
    val environmentMap: MutableMap<OCResolveConfiguration.ModifiableModel, CidrToolEnvironment> = mutableMapOf()
    // forward user defined switches either directly or filter them first
    // todo: in some rare case there can be multiple targets in one OCResolveConfiguration
    for (baseTarget in environment.baseTargetInfos.calculateCppTargets()) {
      if (baseTarget.target.data == null) continue
      val cppModule = extractData<CppModule>(baseTarget.target.data, "cpp") ?: continue
      if (cppModule.cToolchainInfo == null) continue



      println(baseTarget)
      val configSourceFiles: MutableMap<VirtualFile, PerFileCompilerOpts> = mutableMapOf()
      val configLanguages: MutableMap<OCLanguageKind, PerLanguageCompilerOpts> = mutableMapOf()


      val coptsExtractor = UnfilteredCompilerOptions.Builder()
        .registerSingleOrSplitOption("-I").build(cppModule.copts)


      val compilerSwitchesBuilder = selectSwitchBuilder(cppModule.cToolchainInfo!!.compilerVersion)

      // forward user defined switches either directly or filter them first
//      val plainLocalCopts = coptsExtractor.getUninterpretedOptions()
//      if (Registry.`is`("bazel.cpp.sync.workspace.filter.out.incompatible.flags")) {
//        compilerSwitchesBuilder.withSwitches(filterIncompatibleFlags(plainLocalCopts))
//      } else {
//        compilerSwitchesBuilder.withSwitches(plainLocalCopts)
//      }

      // transitiveDefines are sourced from a target's (and transitive deps) "defines" attribute
      cppModule.transitiveDefine.forEach { compilerSwitchesBuilder.withMacro(it) }

      // localIncludes are sourced from -I options in a target's "copts" attribute. They can be
      // arbitrarily declared and may not exist in configResolveData.
      coptsExtractor.getExtractedOptionValues("-I").map{it.URIStringToPathString()}
        .forEach { compilerSwitchesBuilder.withIncludePath(it) }

      // transitiveIncludeDirectories are sourced from CcSkylarkApiProvider.include_directories
      // todo: check isValidHeaderRoot
      cppModule.transitiveIncludeDirectory.map{it.URIStringToPathString()}.forEach { compilerSwitchesBuilder.withIncludePath(it)
        .withHeaderSearchPath(HeadersSearchPath(it,false,HeadersSearchPath.Kind.USER)) }

      // transitiveQuoteIncludeDirectories are sourced from
      // CcSkylarkApiProvider.quote_include_directories
      // todo: isValidHeaderRoot

      cppModule.transitiveQuoteIncludeDirectory.map{it.URIStringToPathString()}.forEach { compilerSwitchesBuilder.withQuoteIncludePath(it)
        .withHeaderSearchPath(HeadersSearchPath(it,false,HeadersSearchPath.Kind.USER)) }

      // transitiveSystemIncludeDirectories are sourced from
      // CcSkylarkApiProvider.system_include_directories
      // Note: We would ideally use -isystem here, but it interacts badly with the switches
      // that get built by ClangUtils::addIncludeDirectories (it uses -I for system libraries).
      cppModule.transitiveSystemIncludeDirectory.map{it.URIStringToPathString()}.forEach { compilerSwitchesBuilder.withSystemIncludePath(it)
        .withHeaderSearchPath(HeadersSearchPath(it,false,HeadersSearchPath.Kind.USER))}

      compilerSwitchesBuilder.withHeaderSearchPath(HeadersSearchPath(environment.project.rootDir.path,false,HeadersSearchPath.Kind.USER))
      if(cppModule.execRoot!=null){
        compilerSwitchesBuilder.withHeaderSearchPath(HeadersSearchPath(cppModule.execRoot!!,false,HeadersSearchPath.Kind.USER))
      }

      val cCompilerSwitches =
        CidrSwitchBuilder().addAllRaw(cppModule.cToolchainInfo!!.cOptions).addAll(compilerSwitchesBuilder.build()).build()
      val cppCompilerSwitches =
        CidrSwitchBuilder().addAllRaw(cppModule.cToolchainInfo!!.cppOptions).addAll(compilerSwitchesBuilder.build()).build()

      val sources = cppModule.sources + cppModule.headers + cppModule.textualHeaders
      for (fileName in sources) {
        val vf = VirtualFileManager.getInstance().findFileByUrl(fileName)
        if (vf != null) {
          val kind: OCLanguageKind? = getDeclaredLanguageKind(environment.project, vf)
          val perFileCompilerOpts = if (kind === CLanguageKind.C) {
            PerFileCompilerOpts(kind, cCompilerSwitches)
          } else {
            PerFileCompilerOpts(CLanguageKind.CPP, cppCompilerSwitches)
          }
          configSourceFiles[vf] = perFileCompilerOpts
        }


        /*
        if (!configLanguages.containsKey(kind)) {
            addConfigLanguageSwitches(
            configLanguages, compilerSettings,
            // If a file isn't found in configSourceFiles (newly created files), CLion uses the
            // configLanguages switches. We want some basic header search roots (genfiles),
            // which are part of every target's iquote directories. See:
            // https://github.com/bazelbuild/bazel/blob/2c493e8a2132d54f4b2fb8046f6bcef11e92cd22/src/main/java/com/google/devtools/build/lib/rules/cpp/CcCompilationHelper.java#L911
            quoteIncludePaths, kind);
          }
        * */
      }
      // generate language-specific switches into configLanguages
      for (language in supportedLanguages) {
        if (!configLanguages.containsKey(language)) {
          addConfigLanguageSwitches(
            configLanguages, cppModule, listOf(), language, cppModule.cToolchainInfo!!.compilerVersion,
          )
        }
      }


      val id = baseTarget.target.id.toString()
      val modelConfig = addConfiguration(
        workspaceModifiable,
        id,
        id,
        File(cppModule.execRoot),// todo: this is the bug
        configLanguages,
        configSourceFiles,
      )
      environmentMap[modelConfig]=createEnvironment(null)
    }
    collectCompilerSettingsInParallel(workspaceModifiable,environmentMap,environment)
    // directly commit
    workspaceModifiable.setClientVersion(1)
    workspaceModifiable.preCommit()
    writeAction {
      workspaceModifiable.commit()
    }
  }

  private fun createEnvironment(info:CToolchainInfo?):CidrToolEnvironment{
    // todo: create environment for msvc
    return CidrToolEnvironment()
  }
  
  
  private fun   collectCompilerSettingsInParallel(
    model: OCWorkspace.ModifiableModel,
    environmentMap:  MutableMap<OCResolveConfiguration.ModifiableModel, CidrToolEnvironment>,
    environment: ProjectSyncHook.ProjectSyncHookEnvironment
  ):List<String> {
    val compilerInfoCache = CompilerInfoCache()
    val tempFilesPool: TempFilesPool = CachedTempFilesPool()
    val session = compilerInfoCache.createSession<Int>(EmptyProgressIndicator())
    val issues = ImmutableList.builder<String>()

    try {
      var i = 0
      for (config in model.getConfigurations()) {
        session.schedule(
          i++,
          config,
          environmentMap[config]!!,
          environment.project.rootDir.path
        )
      }
      val messages = MultiMap<Int, CompilerInfoCache.Message>()
      session.waitForAll(messages)

    } catch (e: Error) {
      session.dispose() // This calls tempFilesPool.clean();
      throw e
    } catch (e: RuntimeException) {
      session.dispose()
      throw e
    }
    tempFilesPool.clean()
    return issues.build()
  }

  override val buildToolId: BuildToolId = bspBuildToolId

  private fun BaseTargetInfos.calculateCppTargets(): List<BaseTargetInfo> = infos.filter { it.target.languageIds.contains("cpp") }

  private fun filterIncompatibleFlags(copts: List<String>): List<String> =
    copts.filter { !it.startsWith("-include ") }
  // "-include somefile.h" doesn't seem to work for some reason. E.g.,
  // "-include cstddef" results in "clang: error: no such file or directory: 'cstddef'"


  fun getDeclaredLanguageKind(project: Project, sourceOrHeaderFile: VirtualFile): OCLanguageKind? {
    val fileName = sourceOrHeaderFile.name
    if (OCFileTypeHelpers.isSourceFile(fileName)) {
      return getLanguageKind(sourceOrHeaderFile)
    }

    if (OCFileTypeHelpers.isHeaderFile(fileName)) {
      return getLanguageKind(getSourceFileForHeaderFile(project, sourceOrHeaderFile))
    }

    return null
  }

  fun getSourceFileForHeaderFile(project: Project, headerFile: VirtualFile): VirtualFile? {
    return runReadAction {
      val roots =
        OCImportGraph.getInstance(project).getAllHeaderRoots(headerFile)
      val headerNameWithoutExtension = headerFile.nameWithoutExtension
      for (root in roots) {
        if (root.nameWithoutExtension == headerNameWithoutExtension) {
          return@runReadAction root
        }
      }
      return@runReadAction null
    }
  }

  private fun getLanguageKind(sourceFile: VirtualFile?): OCLanguageKind {
    val kind = OCFileTypeHelpers.getLanguageKind(sourceFile?.name)
    return kind ?: CLanguageKind.CPP
  }


  private fun addConfigLanguageSwitches(
    configLanguages: MutableMap<OCLanguageKind, PerLanguageCompilerOpts>,
    target: CppModule,
    quoteIncludePaths: List<String>,
    language: OCLanguageKind, version: String
  ) {
    val compilerKind = getCompiler(language, version)
    val compilerSwitchesBuilder = selectSwitchBuilder(version)
    val compiler= if (language === CLanguageKind.C) {
      target.cToolchainInfo!!.cCompiler
    }else{
      target.cToolchainInfo!!.cppCompiler
    }
    quoteIncludePaths.forEach { compilerSwitchesBuilder.withIncludePath(it) }
    configLanguages[language]=PerLanguageCompilerOpts(compilerKind, compiler, compilerSwitchesBuilder.build())

  }

  private fun addConfiguration(
    workspaceModifiable: OCWorkspace.ModifiableModel,
    id: String,
    displayName: String,
    directory: File,
    configLanguages: Map<OCLanguageKind, PerLanguageCompilerOpts>,
    configSourceFiles: Map<VirtualFile, PerFileCompilerOpts>
  ): OCResolveConfiguration.ModifiableModel {
    val config =
      workspaceModifiable.addConfiguration(
        id, displayName, null, OCResolveConfiguration.DEFAULT_FILE_SEPARATORS,
      )
    // write per language model
    for ((key, configForLanguage) in configLanguages) {
      val langSettings = config.getLanguageCompilerSettings(key)
      langSettings.setCompiler(configForLanguage.kind, Paths.get(URI(configForLanguage.compiler)).toFile(), directory)
      langSettings.setCompilerSwitches(configForLanguage.switches)
    }

    for ((key, compilerOpts) in configSourceFiles) {

      val fileCompilerSettings =
        config.addSource(key, compilerOpts.kind)
      fileCompilerSettings.setCompilerSwitches(compilerOpts.switches)

    }

    return config
  }


  private fun String.isClang() = contains("clang")
  private fun String.isAppleClang() = contains("clang") && contains("Apple")
  private fun String.isMSVC() = contains("Microsoft")


  fun selectSwitchBuilder(version: String): CompilerSpecificSwitchBuilder {
    if (version.isAppleClang()) {
      return AppleClangSwitchBuilder();
    }
    if (version.isClang()) {
      return ClangSwitchBuilder();
    }
    if (version.isMSVC()) {
      return MSVCSwitchBuilder();
    }
    // default to gcc
    return GCCSwitchBuilder();
  }

  fun getCompiler(languageKind: OCLanguageKind, version: String): OCCompilerKind {
    if (languageKind !== CLanguageKind.C && languageKind !== CLanguageKind.CPP) {
      return UnknownCompilerKind
    }

    if (version.isMSVC()) {
      return MSVCCompilerKind
    }

    if (version.isClang()) {
      return ClangCompilerKind
    }

    // default to gcc
    return GCCCompilerKind
  }

  private fun String.URIStringToPathString():String{
    return URI(this).path
  }


}


private class PerFileCompilerOpts(
  val kind: OCLanguageKind,
  val switches: CidrCompilerSwitches
)

/** Group compiler options for a specific language.  */
private class PerLanguageCompilerOpts(
  val kind: OCCompilerKind,
  val compiler: String,
  val switches: CidrCompilerSwitches
)
