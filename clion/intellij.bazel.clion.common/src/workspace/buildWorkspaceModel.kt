package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path
import java.util.Objects
import kotlin.sequences.forEach

private val DEFAULT_LANGUAGE_KIND = CLanguageKind.CPP

context(ctx: CcImportContext)
internal fun buildWorkspaceModel(model: OCWorkspace.ModifiableModel, configs: List<CcResolveConfiguration>) {
  for (config in configs) {
    val workspaceConfig = model.addConfiguration(id = config.id, name = config.name, variant = null)

    val settings = config.shared.compilerSettings

    // TODO: port the copts processing i.e. com.google.idea.blaze.cpp.copts.CoptsProcessor
    val cSwitches = CompilerSpecificSwitchBuilder.getBuilder(settings.cCompilerKind).apply {
      appendCompilationContext(config)
      withSwitches(settings.cSwitches)
      withSwitches(config.shared.copts)
      withSwitches(config.shared.conlyopts)
    }.build()

    val cppSwitches = CompilerSpecificSwitchBuilder.getBuilder(settings.cppCompilerKind).apply {
      appendCompilationContext(config)
      withSwitches(settings.cppSwitches)
      withSwitches(config.shared.copts)
      withSwitches(config.shared.cxxopts)
    }.build()

    for (file in config.sources) {
      val languageKind = getDeclaredLanguageKind(file)
      val fileConfig = workspaceConfig.addSource(file, languageKind)

      if (languageKind == CLanguageKind.C) {
        fileConfig.setCompiler(settings.cCompilerKind, settings.cCompiler.toFile(), ctx.execroot.toFile())
        fileConfig.setCompilerSwitches(cSwitches)
      }

      if (languageKind == CLanguageKind.CPP) {
        fileConfig.setCompiler(settings.cppCompilerKind, settings.cppCompiler.toFile(), ctx.execroot.toFile())
        fileConfig.setCompilerSwitches(cppSwitches)
      }
    }

    workspaceConfig.getLanguageCompilerSettings(CLanguageKind.C).apply {
      setCompiler(settings.cCompilerKind, settings.cCompiler.toFile(), ctx.execroot.toFile())
      setCompilerSwitches(cSwitches)
    }

    workspaceConfig.getLanguageCompilerSettings(CLanguageKind.CPP).apply {
      setCompiler(settings.cppCompilerKind, settings.cppCompiler.toFile(), ctx.execroot.toFile())
      setCompilerSwitches(cppSwitches)
    }
  }
}

context(ctx: CcImportContext)
private fun CompilerSpecificSwitchBuilder.appendCompilationContext(config: CcResolveConfiguration) {
  config.shared.transitiveDefines.forEach(::withMacro)
  config.shared.transitiveIncludes.getOutputLocations().resolve().forEach(::withIncludePath)
  config.shared.transitiveQuoteIncludes.getOutputLocations().resolve().forEach(::withQuoteIncludePath)
  config.shared.transitiveSystemIncludes.getOutputLocations().resolve().forEach(::withSystemIncludePath)
}

private fun getDeclaredLanguageKind(sourceOrHeaderFile: VirtualFile): OCLanguageKind {
  val name = sourceOrHeaderFile.name

  if (OCFileTypeHelpers.isSourceFile(name)) {
    return OCFileTypeHelpers.getLanguageKind(name) ?: DEFAULT_LANGUAGE_KIND
  }

  return DEFAULT_LANGUAGE_KIND
}

context(ctx: CcImportContext)
private fun Sequence<OutputLocation>.resolve(): Sequence<String> {
  // TODO: again, do we want to report errors when a resolve fails?
  return mapNotNull(ctx::resolve).map(Objects::toString)
}
