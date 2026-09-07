package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import org.jetbrains.bazel.clion.BazelClionBundle

context(ctx: CcImportContext)
internal fun collectCompilerInfo(model: OCWorkspace.ModifiableModel, configurations: List<CcResolveConfiguration>) {

  // TODO: can we use a real indicator here?
  val session = CompilerInfoCache().createSession<String>(EmptyProgressIndicator())

  try {
    for (config in configurations) {
      val configModel = model.getConfigurationById(config.id) ?: continue
      session.schedule(config.id, configModel, config.shared.compilerSettings.toolEnvironment, ctx.execroot.toString())
    }

    val messages = MultiMap<String, CompilerInfoCache.Message>()
    session.waitForAll(messages)

    reportProblems(messages)
  } finally {
      session.dispose()
  }
}

context(ctx: CcImportContext)
private fun reportProblems(problems: MultiMap<String, CompilerInfoCache.Message>) {
  if (problems.isEmpty()) return

  val description = problems.entrySet().joinToString("\n") { (configId, messages) ->
    messages.joinToString("\n") { problem -> "$configId: ${problem.type}: ${problem.text.trim()}" }
  }

  ctx.reportEvent(
    message = BazelClionBundle.message("cc.compiler.info.failed", problems.keySet().size),
    description = description,
    severity = MessageEvent.Kind.WARNING,
  )
}
