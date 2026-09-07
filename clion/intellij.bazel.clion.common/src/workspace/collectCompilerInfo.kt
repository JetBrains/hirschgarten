package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import org.jetbrains.bazel.clion.BazelClionBundle

context(ctx: CcImportContext)
internal fun collectCompilerInfo(
  session: CompilerInfoCache.Session<String>,
  model: OCWorkspace.ModifiableModel,
  configurations: List<CcResolveConfiguration>,
) {
  for (config in configurations) {
    val configModel = model.getConfigurationById(config.id) ?: continue
    session.schedule(config.id, configModel, config.shared.compilerSettings.toolEnvironment, ctx.execroot.toString())
  }

  val messages = MultiMap<String, CompilerInfoCache.Message>()
  session.waitForAll(messages)

  reportProblems(messages)
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
