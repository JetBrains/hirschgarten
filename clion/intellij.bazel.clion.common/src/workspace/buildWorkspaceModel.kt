@file:Suppress("IO_FILE_USAGE") // required by API

package org.jetbrains.bazel.clion.workspace

import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import org.jetbrains.bazel.clion.workspace.copts.applyCopts
import org.jetbrains.bsp.protocol.OutputLocation
import java.util.Objects

private val DEFAULT_LANGUAGE_KIND = CLanguageKind.CPP

context(ctx: CcImportContext)
internal fun buildWorkspaceModel(model: OCWorkspace.ModifiableModel, configs: List<CcResolveConfiguration>) {
  for (config in configs) {
    val workspaceConfig = model.addConfiguration(id = config.id.encode(), name = config.name, variant = null)

    val settings = config.shared.compilerSettings

    val cSwitches = buildSwitches(
      config,
      settings.cCompilerKind,
      settings.cSwitches,
      config.shared.copts,
      config.shared.conlyopts,
    )

    val cppSwitches = buildSwitches(
      config,
      settings.cppCompilerKind,
      settings.cppSwitches,
      config.shared.copts,
      config.shared.cxxopts,
    )

    for (file in config.sources) {
      val languageKind = getDeclaredLanguageKind(file)
      val fileConfig = workspaceConfig.addSource(file.url, languageKind)

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

/** Builds the switches of one language. Every list of [options] passes the copts processing. */
context(_: CcImportContext)
private fun buildSwitches(
  config: CcResolveConfiguration,
  kind: OCCompilerKind,
  vararg options: List<String>,
): CidrCompilerSwitches = CompilerSpecificSwitchBuilder.getBuilder(kind).apply {
  appendCompilationContext(config)
  options.forEach { applyCopts(kind, it) }
}.build()

context(ctx: CcImportContext)
private fun CompilerSpecificSwitchBuilder.appendCompilationContext(config: CcResolveConfiguration) {
  config.shared.transitiveDefines.forEach(::withMacro)
  config.shared.transitiveIncludes.getOutputLocations().resolve().forEach(::withIncludePath)
  config.shared.transitiveQuoteIncludes.getOutputLocations().resolve().forEach(::withQuoteIncludePath)
  config.shared.transitiveSystemIncludes.getOutputLocations().resolve().forEach(::withSystemIncludePath)
}

private fun getDeclaredLanguageKind(sourceOrHeaderFile: VirtualFileUrl): OCLanguageKind {
  val name = sourceOrHeaderFile.fileName

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
