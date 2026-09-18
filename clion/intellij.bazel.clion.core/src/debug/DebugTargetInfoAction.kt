package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.clion.workspace.presentable
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.isFull

/** Asks for a target, then reports every section of it. */
internal class DebugTargetInfoAction : BazelDebugAction() {

  override val showOutputInEditor: Boolean
    get() = true

  override suspend fun exec(project: Project): Any {
    val snapshot = snapshotOrFail(project)

    val summaries = snapshot.targets.allTargets(TargetLoadOptions.SUMMARY).toList()
    if (summaries.isEmpty()) {
      fail("no target found in the workspace snapshot")
    }

    val chosen = chooseInPopup(project, BazelCLionCoreBundle.message("debug.popup.targets.title"), summaries) {
      "${it.kind.kind} rule ${it.key.presentable()}"
    }

    val target = snapshot.targets.findTargetByKey(chosen.key, TargetLoadOptions.ALL)
      ?: fail("target ${chosen.key} is absent from the workspace snapshot")

    if (!target.isFull) {
      fail("target ${chosen.key} did not load every section")
    }

    return target.toJsonMap()
  }
}

private fun BuildTarget.toJsonMap(): Map<String, Any?> {
  return mapOf(
    "key" to key,
    "kind" to kind,
    "base_directory" to baseDirectory,
    "generator_name" to generatorName,
    "is_workspace" to isWorkspace,
    "is_test_only" to isTestOnly,
    "tags" to tags,
    "dependencies" to dependencies,
    "sources" to sources,
    "generated_sources" to generatedSources,
    "resources" to resources,
    "data" to data.associate { it.javaClass.simpleName to it.toJsonMap() },
  )
}

private fun BuildTargetData.toJsonMap(): Map<String, Any?> {
  return when (this) {
    is CcBuildTarget -> toJsonMap()
    is CcToolchainBuildTarget -> toJsonMap()
    else -> emptyMap()
  }
}
private fun CcBuildTarget.toJsonMap(): Map<String, Any?> {
  return mapOf(
    "rule_context" to ruleContext?.let {
      mapOf(
        "headers" to it.headers,
        "textualHeaders" to it.textualHeaders,
        "copts" to it.copts,
        "conlyopts" to it.conlyopts,
        "cxxopts" to it.cxxopts,
        "args" to it.args,
        "include_prefix" to it.includePrefix,
        "strip_include_prefix" to it.stripIncludePrefix,
      )
    },
    "compilationContext" to linkedMapOf(
      "headers" to compilationContext.headers,
      "defines" to compilationContext.defines,
      "includes" to compilationContext.includes,
      "quote_includes" to compilationContext.quoteIncludes,
      "system_includes" to compilationContext.systemIncludes,
    ),
  )
}

private fun CcToolchainBuildTarget.toJsonMap(): Map<String, Any?> {
  return mapOf(
    "target_name" to targetName,
    "compiler_name" to compilerName,
    "c_compiler" to cCompiler,
    "cpp_compiler" to cppCompiler,
    "c_option" to cOption,
    "cpp_option" to cppOption,
    "built_in_include_directories" to builtInIncludeDirectories,
    "sysroot" to sysroot,
    "c_environment" to cEnvironment.toSortedMap(),
    "cpp_environment" to cppEnvironment.toSortedMap(),
  )
}
