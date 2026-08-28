package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.GCCSwitchBuilder
import org.jetbrains.bsp.protocol.OutputLocation
import java.util.Objects
import kotlin.sequences.forEach

private val DEFAULT_LANGUAGE_KIND = CLanguageKind.CPP

context(ctx: CcImportContext)
internal fun buildWorkspaceModel(configs: List<CcResolveConfiguration>, model: OCWorkspace.ModifiableModel) {
  for (config in configs) {
    val workspaceConfig = model.addConfiguration(id = config.id, name = config.name, variant = null)

    // TODO: use detected compiler for the configuration
    val switchBuilder = GCCSwitchBuilder()

    config.shared.transitiveDefines.forEach(switchBuilder::withMacro)
    config.shared.transitiveIncludes.getOutputLocations().resolve().forEach(switchBuilder::withIncludePath)
    config.shared.transitiveQuoteIncludes.getOutputLocations().resolve().forEach(switchBuilder::withQuoteIncludePath)
    config.shared.transitiveSystemIncludes.getOutputLocations().resolve().forEach(switchBuilder::withSystemIncludePath)

    val cSwitches = GCCSwitchBuilder().apply {
      withSwitches(switchBuilder.build())
      withSwitches(config.shared.compilerSettings.cSwitches)
    }.build()

    val cppSwitches = GCCSwitchBuilder().apply {
      withSwitches(switchBuilder.build())
      withSwitches(config.shared.compilerSettings.cppSwitches)
    }.build()

    // TODO: port the copts processing i.e. com.google.idea.blaze.cpp.copts.CoptsProcessor

    for (file in config.sources) {
      val languageKind = getDeclaredLanguageKind(file)
      val fileConfig = workspaceConfig.addSource(file, languageKind)

      if (languageKind == CLanguageKind.C) {
        // TODO: use detected compiler for the configuration
        fileConfig.setCompiler(GCCCompilerKind, config.shared.compilerSettings.cCompiler.toFile(), ctx.execroot.toFile())
        fileConfig.setCompilerSwitches(cSwitches)
      }

      if (languageKind == CLanguageKind.CPP) {
        // TODO: use detected compiler for the configuration
        fileConfig.setCompiler(GCCCompilerKind, config.shared.compilerSettings.cppCompiler.toFile(), ctx.execroot.toFile())
        fileConfig.setCompilerSwitches(cppSwitches)
      }
    }

    workspaceConfig.getLanguageCompilerSettings(CLanguageKind.C).apply {
      setCompiler(GCCCompilerKind, config.shared.compilerSettings.cCompiler.toFile(), ctx.execroot.toFile())
      setCompilerSwitches(cSwitches)
    }

    workspaceConfig.getLanguageCompilerSettings(CLanguageKind.CPP).apply {
      setCompiler(GCCCompilerKind, config.shared.compilerSettings.cppCompiler.toFile(), ctx.execroot.toFile())
      setCompilerSwitches(cppSwitches)
    }
  }
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
