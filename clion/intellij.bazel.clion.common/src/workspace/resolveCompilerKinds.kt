package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.UnknownCompilerKind
import org.jetbrains.bazel.clion.BazelClionBundle
import java.nio.file.Path

/** [UnknownCompilerKind] builds a GCC switch builder, which is what the import used for every compiler before. */
internal fun OCCompilerKind?.orUnknown(): OCCompilerKind = this ?: UnknownCompilerKind

context(ctx: CcImportContext)
internal fun resolveCompilerKinds(
  project: Project,
  session: CompilerInfoCache.Session<String>,
  configurations: List<CcResolveConfiguration>,
  environments: CcToolEnvironments,
): Map<Path, OCCompilerKind> {
  val kinds = mutableMapOf<Path, OCCompilerKind>()

  for (settings in configurations.map { it.shared.compilerSettings }.distinct()) {
    val environment = environments.of(settings.environment)
    for (compiler in listOf(settings.cCompiler, settings.cppCompiler)) {
      kinds.getOrPut(compiler) { session.resolveCompiler(project, compiler, environment) }
    }
  }

  reportUnknownKinds(kinds)

  return kinds
}

context(ctx: CcImportContext)
private fun reportUnknownKinds(kinds: Map<Path, OCCompilerKind>) {
  val unknown = kinds.filterValues { it.id == OCCompilerId.UNKNOWN }.keys
  if (unknown.isEmpty()) return

  ctx.reportEvent(
    message = BazelClionBundle.message("cc.compiler.kind.unknown", unknown.size),
    description = unknown.joinToString("\n") { it.toString() },
    severity = MessageEvent.Kind.WARNING,
  )
}
