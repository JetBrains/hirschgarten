package org.jetbrains.bazel.clion.workspace

import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.io.IOException
import java.nio.file.Path

private const val PROC_SELF_CWD = "/proc/self/cwd"

// placeholders that the apple_support compiler wrapper replaces
private const val EXECUTION_ROOT_PLACEHOLDER = "__BAZEL_EXECUTION_ROOT__"
private const val EXECUTION_ROOT_CANONICAL_PLACEHOLDER = "__BAZEL_EXECUTION_ROOT_CANONICAL__"
private const val XCODE_DEVELOPER_DIR_PLACEHOLDER = "__BAZEL_XCODE_DEVELOPER_DIR__"
private const val XCODE_SDKROOT_PLACEHOLDER = "__BAZEL_XCODE_SDKROOT__"

data class CcCompilerInfo(
  val cCompiler: Path,
  val cCompilerKind: OCCompilerKind,
  val cppCompiler: Path,
  val cppCompilerKind: OCCompilerKind,
  val cSwitches: List<String>,
  val cppSwitches: List<String>,
  val name: String,
  val environment: Map<String, String>,
  val builtinIncludes: OutputLocationCollection,
  val sysroot: OutputLocation?,
) {

  // TODO: create specialized environment for clang-cl and MSVC
  val toolEnvironment: CidrToolEnvironment by lazy { createToolEnvironment(environment) }
}

private class CcToolEnvironment(private val environment: Map<String, String>) : CidrToolEnvironment() {

  override fun prepare(commandLine: GeneralCommandLine, prepareFor: PrepareFor) {
    super.prepare(commandLine, prepareFor)
    commandLine.environment.putAll(environment)
  }
}

@ApiStatus.Internal
context(ctx: CcImportContext)
suspend fun buildCompilerSettings(): Map<WorkspaceTargetKey, CcCompilerInfo> = coroutineScope {
  val result = mutableMapOf<WorkspaceTargetKey, CcCompilerInfo>()

  val compilerResolver = CcCompilerResolver(ctx)
  val executionRootPlaceholders = createExecutionRootPlaceholders()

  for (target in ctx.snapshot.targets.allTargets()) {
    val toolchainInfo = target.extractData<CcToolchainBuildTarget>() ?: continue
    val placeholders = executionRootPlaceholders + createXcodePlaceholders(toolchainInfo.xcodeInfo)

    val environment = createEnvironment(
      toolchainInfo.cEnvironment,
      toolchainInfo.cppEnvironment,
      createXcodeEnvironment(toolchainInfo)
    )

    val toolEnvironment = createToolEnvironment(environment)

    val cCompiler = compilerResolver.resolve(toolchainInfo.cCompiler, toolEnvironment) ?: continue
    val cppCompiler = compilerResolver.resolve(toolchainInfo.cppCompiler, toolEnvironment) ?: continue

    val compilerInfo = CcCompilerInfo(
      cCompiler = cCompiler.path,
      cppCompiler = cppCompiler.path,
      cCompilerKind = cCompiler.kind,
      cppCompilerKind = cppCompiler.kind,
      cSwitches = expandPlaceholders(toolchainInfo.cOption.asList(), placeholders),
      cppSwitches = expandPlaceholders(toolchainInfo.cppOption.asList(), placeholders),
      name = toolchainInfo.compilerName,
      environment = environment,
      builtinIncludes = toolchainInfo.builtInIncludeDirectories,
      sysroot = toolchainInfo.sysroot,
    )

    result[target.key] = compilerInfo
  }

  compilerResolver.reportProblems()

  result
}

context(ctx: CcImportContext)
private fun createEnvironment(vararg environments: Map<String, String>): Map<String, String> {
  val merged = mutableMapOf<String, String>()
  for (environment in environments) {
    for ((key, value) in environment.entries) {
      merged[key] = resolveProcSelfCwd(value)
    }
  }

  return merged
}

context(ctx: CcImportContext)
private fun resolveProcSelfCwd(value: String): String {
  if (!value.startsWith(PROC_SELF_CWD)) return value

  val resolved = ctx.resolveExecutionRootPath(value) ?: return value
  return resolved.toString()
}

private fun createToolEnvironment(environment: Map<String, String>): CidrToolEnvironment {
  return if (environment.isEmpty()) {
    CidrToolEnvironment()
  }
  else {
    CcToolEnvironment(environment)
  }
}

private fun createXcodeEnvironment(info: CcToolchainBuildTarget): Map<String, String> {
  if (info.xcodeInfo == null) return emptyMap()

  return mapOf(
    "DEVELOPER_DIR" to info.xcodeInfo.developerDir,
    "SDKROOT" to info.xcodeInfo.sdkRoot,
  )
}

context(ctx: CcImportContext)
private fun createExecutionRootPlaceholders(): Map<String, String> {
  val canonical = try {
    ctx.execroot.toRealPath()
  }
  catch (_: IOException) {
    ctx.execroot
  }

  return mapOf(
    EXECUTION_ROOT_PLACEHOLDER to ctx.execroot.toString(),
    EXECUTION_ROOT_CANONICAL_PLACEHOLDER to canonical.toString(),
  )
}

private fun createXcodePlaceholders(info: CcToolchainBuildTarget.XcodeInfo?): Map<String, String> {
  if (info == null) return emptyMap()

  return mapOf(
    XCODE_DEVELOPER_DIR_PLACEHOLDER to info.developerDir,
    XCODE_SDKROOT_PLACEHOLDER to info.sdkRoot,
  )
}

/** Replaces each placeholder in the options. A placeholder can be a part of an option. */
private fun expandPlaceholders(options: List<String>, placeholders: Map<String, String>): List<String> {
  return options.map { option ->
    placeholders.entries.fold(option) { result, (placeholder, value) -> result.replace(placeholder, value) }
  }
}
