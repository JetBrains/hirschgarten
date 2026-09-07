package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.isUnknown
import com.jetbrains.cidr.lang.workspace.compiler.resolver.OCCompilerResolver
import org.jetbrains.bazel.clion.BazelClionBundle
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

data class CcCompilerInfo(
  val cCompiler: Path,
  val cCompilerKind: OCCompilerKind,
  val cppCompiler: Path,
  val cppCompilerKind: OCCompilerKind,
  val cSwitches: List<String>,
  val cppSwitches: List<String>,
  val name: String,
  val environment: Map<String, String>,
  val toolEnvironment: CidrToolEnvironment,
  val builtinIncludes: OutputLocationCollection,
  val sysroot: OutputLocation?,
)

private class CcToolEnvironment(private val environment: Map<String, String>) : CidrToolEnvironment() {

  override fun prepare(commandLine: GeneralCommandLine, prepareFor: PrepareFor) {
    super.prepare(commandLine, prepareFor)
    commandLine.environment.putAll(environment)
  }
}

@OptIn(ExperimentalStdlibApi::class)
context(ctx: CcImportContext)
internal fun buildCompilerSettings(): Map<WorkspaceTargetKey, CcCompilerInfo> {
  val result = mutableMapOf<WorkspaceTargetKey, CcCompilerInfo>()
  val cache = mutableMapOf<OutputLocation, Pair<Path, OCCompilerKind>?>()

  for (target in ctx.snapshot.targets.allTargets()) {
    val toolchainInfo = target.extractData<CcToolchainBuildTarget>() ?: continue

    // TODO: port environment processing i.e. com.google.idea.blaze.cpp.environment.EnvironmentProcessor
    val environment = mergeEnvironments(toolchainInfo.cEnvironment, toolchainInfo.cppEnvironment)
    val toolEnvironment = createToolEnvironment(environment)

    val (cCompiler, cCompilerKind) = cache.getOrPutIfMissing(toolchainInfo.cCompiler) {
      resolveCompiler(toolchainInfo.cCompiler, toolEnvironment)
    } ?: continue

    val (cppCompiler, cppCompilerKind) = cache.getOrPutIfMissing(toolchainInfo.cppCompiler) {
      resolveCompiler(toolchainInfo.cppCompiler, toolEnvironment)
    } ?: continue

    val compilerInfo = CcCompilerInfo(
      cCompiler = cCompiler,
      cppCompiler = cppCompiler,
      cCompilerKind = cCompilerKind,
      cppCompilerKind = cppCompilerKind,
      cSwitches = toolchainInfo.cOption,
      cppSwitches = toolchainInfo.cppOption,
      name = toolchainInfo.compilerName,
      environment = environment,
      toolEnvironment = toolEnvironment,
      builtinIncludes = toolchainInfo.builtInIncludeDirectories,
      sysroot = toolchainInfo.sysroot,
    )

    result[target.key] = compilerInfo
  }

  reportProblems(cache)

  return result
}

context(ctx: CcImportContext)
private fun resolveCompiler(location: OutputLocation, environment: CidrToolEnvironment): Pair<Path, OCCompilerKind>? {
  val path = ctx.resolve(location) ?: return null
  return path to OCCompilerResolver.resolve(ctx.project, path, environment)
}

private fun mergeEnvironments(vararg environments: Map<String, String>): Map<String, String> {
  val merged = mutableMapOf<String, String>()
  for (environment in environments) {
    merged.putAll(environment)
  }

  return merged
}

private fun createToolEnvironment(environment: Map<String, String>): CidrToolEnvironment {
  return if (environment.isEmpty()) {
    CidrToolEnvironment()
  }
  else {
    CcToolEnvironment(environment)
  }
}

context(ctx: CcImportContext)
private fun reportProblems(compilers: Map<OutputLocation, Pair<Path, OCCompilerKind>?>) {
  val problems = compilers.entries.mapNotNull { (location, result) ->
    when {
      result == null -> BazelClionBundle.message("cc.compiler.path.resolve.failed", location)
      result.second.isUnknown() -> BazelClionBundle.message("cc.compiler.kind.resolve.failed", location)
      else -> null
    }
  }

  if (problems.isEmpty()) return

  ctx.reportEvent(
    message = BazelClionBundle.message("cc.compiler.resolve.failed"),
    description = problems.joinToString("\n"),
    severity = MessageEvent.Kind.WARNING,
  )
}
