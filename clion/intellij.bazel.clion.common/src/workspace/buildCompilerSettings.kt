package org.jetbrains.bazel.clion.workspace

import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

private const val PROC_SELF_CWD = "/proc/self/cwd"

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
suspend fun buildCompilerSettings(): Map<WorkspaceTargetKey, CcCompilerInfo> {
  val result = mutableMapOf<WorkspaceTargetKey, CcCompilerInfo>()
  val resolver = CcCompilerResolver(ctx)

  for (target in ctx.snapshot.targets.allTargets()) {
    val toolchainInfo = target.extractData<CcToolchainBuildTarget>() ?: continue

    val environment = createEnvironment(toolchainInfo.cEnvironment, toolchainInfo.cppEnvironment)

    val cCompiler = resolver.resolve(toolchainInfo.cCompiler) ?: continue
    val cppCompiler = resolver.resolve(toolchainInfo.cppCompiler) ?: continue

    val compilerInfo = CcCompilerInfo(
      cCompiler = cCompiler.path,
      cppCompiler = cppCompiler.path,
      cCompilerKind = cCompiler.kind,
      cppCompilerKind = cppCompiler.kind,
      cSwitches = toolchainInfo.cOption,
      cppSwitches = toolchainInfo.cppOption,
      name = toolchainInfo.compilerName,
      environment = environment,
      builtinIncludes = toolchainInfo.builtInIncludeDirectories,
      sysroot = toolchainInfo.sysroot,
    )

    result[target.key] = compilerInfo
  }

  resolver.reportProblems()

  return result
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
