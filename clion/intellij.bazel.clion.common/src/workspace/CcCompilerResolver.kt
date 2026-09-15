@file:OptIn(ExperimentalStdlibApi::class)

package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.isUnknown
import com.jetbrains.cidr.lang.workspace.compiler.resolver.OCCompilerResolver
import org.jetbrains.bazel.clion.BazelClionBundle
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path

internal class CcCompilerResolver(private val ctx: CcImportContext) {

  data class Result(val kind: OCCompilerKind, val path: Path)

  private val cache = mutableMapOf<OutputLocation, Result?>()

  // for version resolution, the environment does not really matter
  private val environment = CidrToolEnvironment()

  fun resolve(location: OutputLocation): Result? {
    return cache.getOrPutIfMissing(location) { doResolve(location) }
  }

  private fun doResolve(location: OutputLocation): Result? {
    val path = ctx.outputResolver.resolve(location) ?: return null
    val kind = OCCompilerResolver.resolve(ctx.project, path, environment)

    return Result(mapKind(kind), path)
  }

  private fun mapKind(kind: OCCompilerKind): OCCompilerKind {
    return when (kind.getId()) {
      OCCompilerId.CLANG -> CcCompilerKind.CLANG
      OCCompilerId.GCC -> CcCompilerKind.GCC
      else -> kind
    }
  }

  fun reportProblems() {
    val problems = cache.entries.mapNotNull { (location, result) ->
      when {
        result == null -> BazelClionBundle.message("cc.compiler.path.resolve.failed", location)
        result.kind.isUnknown() -> BazelClionBundle.message("cc.compiler.kind.resolve.failed", location)
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
}
