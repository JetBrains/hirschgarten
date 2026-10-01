@file:OptIn(ExperimentalStdlibApi::class)

package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.isUnknown
import com.jetbrains.cidr.lang.workspace.compiler.resolver.OCCompilerResolver
import org.jetbrains.bazel.clion.BazelCLionCommonBundle
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path

internal class CcCompilerResolver(private val ctx: CcImportContext) {

  data class Result(val kind: OCCompilerKind, val path: Path)

  private val cache = mutableMapOf<OutputLocation, Result?>()

  fun resolve(location: OutputLocation, environment: CidrToolEnvironment): Result? {
    return cache.getOrPutIfMissing(location) { doResolve(location, environment) }
  }

  private fun doResolve(location: OutputLocation, environment: CidrToolEnvironment): Result? {
    val path = ctx.resolveOutputLocation(location) ?: return null
    val kind = OCCompilerResolver.resolve(ctx.project, path, environment)

    return Result(mapKind(kind), path)
  }

  private fun mapKind(kind: OCCompilerKind): OCCompilerKind {
    return when (kind.getId()) {
      OCCompilerId.CLANG -> CcCompilerKind.CLANG
      OCCompilerId.GCC -> CcCompilerKind.GCC
      OCCompilerId.APPLE_CLANG -> CcCompilerKind.APPLE_CLANG
      else -> kind
    }
  }

  fun reportProblems() {
    val problems = cache.entries.mapNotNull { (location, result) ->
      val compilerPath = ctx.resolveOutputLocation(location).toString()

      when {
        result == null -> BazelCLionCommonBundle.message("cc.compiler.path.resolve.failed", compilerPath)
        result.kind.isUnknown() -> BazelCLionCommonBundle.message("cc.compiler.kind.resolve.failed", compilerPath)
        else -> null
      }
    }

    if (problems.isEmpty()) return

    ctx.reportEvent(
      message = BazelCLionCommonBundle.message("cc.compiler.resolve.failed"),
      description = problems.joinToString("\n"),
      severity = MessageEvent.Kind.WARNING,
    )
  }
}
