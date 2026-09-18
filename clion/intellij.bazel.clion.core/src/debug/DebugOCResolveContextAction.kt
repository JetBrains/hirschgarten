package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.jetbrains.bazel.clion.BazelClionCoreBundle
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.clion.workspace.getCcIdentifier
import kotlin.collections.orEmpty

private val CC_LANGUAGES = listOf(CLanguageKind.C, CLanguageKind.CPP)

/**
 * Asks for a C/C++ resolve configuration, then reports it.
 *
 * It reads the committed [OCWorkspace], so it shows the settings that code resolution uses.
 */
internal class DebugOCResolveContextAction : BazelDebugAction() {

  override val showOutputInEditor: Boolean
    get() = true

  override suspend fun exec(project: Project): Any {
    val configurations = readAction { OCWorkspace.getInstance(project).getConfigurations(CC_CLIENT_KEY) }
    if (configurations.isEmpty()) {
      fail("no $CC_CLIENT_KEY resolve configuration found, the project is not synced")
    }

    val chosen = chooseInPopup(
      project,
      BazelClionCoreBundle.message("debug.popup.resolve.configurations.title"),
      configurations,
    ) { it.displayName }

    return chosen.toJsonMap()
  }
}

private fun OCResolveConfiguration.toJsonMap(): Map<String, Any?> {
  return mapOf(
    "unique_id" to uniqueId,
    "display_name" to displayName,
    "cc_identifier" to getCcIdentifier(),
    "variant" to variant?.toString(),
    "sources" to sources,
    "languages" to CC_LANGUAGES.associate { it.displayName to getCompilerSettings(it).toJsonMap() },
  )
}

private fun OCCompilerSettings.toJsonMap(): Map<String, Any?> {
  return mapOf(
    "compiler_kind" to compilerKind?.toString(),
    "compiler_executable" to compilerExecutable,
    "compiler_working_dir" to compilerWorkingDir,
    "switches" to compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty(),
    "headers_search_paths" to headersSearchPaths.map { it.toString() },
    "implicit_includes" to implicitIncludeUrls,
    "preprocessor_defines" to preprocessorDefines,
  )
}
