package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import kotlin.collections.orEmpty

data class CcResolveConfiguration(
  val id: String,
  val name: @NlsSafe String,
  val shared: EquivalenceClass,
  val targets: List<WorkspaceTargetKey>,
  val sources: List<VirtualFile>,
) {

  /** Represents a set of compiler settings that are shared across multiple targets. */
  data class EquivalenceClass(
    val configuration: WorkspaceConfigurationId,
    val compilerSettings: CcCompilerInfo,

    val copts: List<String>,
    val conlyopts: List<String>,
    val cxxopts: List<String>,

    val transitiveIncludes: OutputLocationCollection,
    val transitiveQuoteIncludes: OutputLocationCollection,
    val transitiveSystemIncludes: OutputLocationCollection,

    val transitiveDefines: List<String>,
  )
}

context(ctx: CcImportContext)
internal fun buildEquivalenceClasses(target2Compiler: Map<WorkspaceTargetKey, CcCompilerInfo?>): List<CcResolveConfiguration> {
  val equivalenceClasses = MultiMap<CcResolveConfiguration.EquivalenceClass, WorkspaceTargetKey>()

  for (target in ctx.snapshot.targets.allTargets()) {
    val info = target.extractData<CcBuildTarget>() ?: continue

    val configuration = CcResolveConfiguration.EquivalenceClass(
      configuration = target.key.configuration,
      compilerSettings = target2Compiler[target.key] ?: continue,
      copts = info.ruleContext?.copts.orEmpty(),
      conlyopts = info.ruleContext?.conlyopts.orEmpty(),
      cxxopts = info.ruleContext?.cxxopts.orEmpty(),
      transitiveIncludes = info.compilationContext.includes,
      transitiveQuoteIncludes = info.compilationContext.quoteIncludes,
      transitiveSystemIncludes = info.compilationContext.systemIncludes,
      transitiveDefines = info.compilationContext.defines,
    )

    equivalenceClasses.putValue(configuration, target.key)
  }

  // TODO: report discovered C configurations here, format: "%s unique C configurations, %s C targets"

  return equivalenceClasses.entrySet().map { entry ->
    CcResolveConfiguration(
      shared = entry.key,
      targets = entry.value.toList(),
      sources = collectSources(entry.value),
      // TODO: introduce equivalence to com.google.idea.blaze.cpp.BlazeResolveConfigurationID, i.e. CcResolveConfigurationID
      id = computeDisplayName(entry.value),
      name = computeDisplayName(entry.value),
    )
  }
}

context(ctx: CcImportContext)
private fun collectSources(targets: Collection<WorkspaceTargetKey>): List<VirtualFile> {
  return targets.asSequence()
    .mapNotNull { ctx.snapshot.targetGraph.findTargetByKey(it) }
    .mapNotNull { ctx.snapshot.targets.findTargetByKey(it, TargetLoadOptions.MINIMAL) }
    .flatMap { target -> target.sources.getFiles() + target.generatedSources.getFiles() }
    // TODO: do we need to call this with refresh true, or switch to URLs?
    .mapNotNull { VfsUtil.findFile(it, /* refreshIfNeeded = */ false) }
    .toList()
}

private fun computeDisplayName(targets: Collection<WorkspaceTargetKey>): String {
  val minTargetKey = targets.minBy { it.label }

  return buildString {
    append(minTargetKey.label)

    // on resolve configuration can cover multiple Bazel configurations with same compiler option
    if (!minTargetKey.configuration.shortChecksum.isNullOrBlank()) {
      append(" (${minTargetKey.configuration.shortChecksum})")
    }

    if (targets.size > 1) {
      append(" and ${targets.size - 1} other target(s)")
    }
  }
}
