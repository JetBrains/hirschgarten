package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import org.jetbrains.bazel.clion.BazelClionBundle

context(ctx: CcImportContext)
internal fun collectCompilerInfo(model: OCWorkspace.ModifiableModel) {
  val compilerInfoCache = CompilerInfoCache()

  // TODO: can we use a real indicator here?
  val session = compilerInfoCache.createSession<Int>(EmptyProgressIndicator())

  val messages = MultiMap<Int, CompilerInfoCache.Message>()
  try {
    for ((i, config) in model.configurations.withIndex()) {
      session.schedule(i, config, CidrToolEnvironment(), ctx.execroot.toString())
    }

    session.waitForAll(messages)
  }
  catch (ex: Throwable) {
    session.dispose()
    throw ex
  }

  reportProblems(messages)
}

context(ctx: CcImportContext)
private fun reportProblems(problems: MultiMap<Int, CompilerInfoCache.Message>) {
  if (problems.isEmpty()) return

  val description = problems.values().joinToString("\n") { problem -> "${problem.type}: ${problem.text.trim()}" }

  ctx.reportEvent(
    message = BazelClionBundle.message("cc.compiler.info.failed", problems.keySet().size),
    description = description,
    severity = MessageEvent.Kind.WARNING,
  )
}
