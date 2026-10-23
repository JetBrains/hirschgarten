package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.BazelCLionCommonBundle
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import kotlin.collections.orEmpty

private const val CC_CONFIGURATION_MARKER = "BAZEL_CC"

data class CcResolveConfiguration(
  val id: Identifier,
  val name: @NlsSafe String,
  val shared: EquivalenceClass,
  val targets: List<WorkspaceTargetKey>,
  val sources: List<VirtualFileUrl>,
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

  /** Wrapper class for the unique identifier for an [OCResolveConfiguration]. */
  data class Identifier(
    val hashCode: Int,
    val configurationId: WorkspaceConfigurationId,
  )
}

@ApiStatus.Internal
context(ctx: CcImportContext)
fun buildEquivalenceClasses(target2Compiler: Map<WorkspaceTargetKey, CcCompilerInfo?>): List<CcResolveConfiguration> {
  val equivalenceClasses = MultiMap<CcResolveConfiguration.EquivalenceClass, WorkspaceTargetKey>()

  for (target in ctx.snapshot.targets.allTargets()) {
    val info = target.extractData<CcBuildTarget>() ?: continue

    val configuration = CcResolveConfiguration.EquivalenceClass(
      configuration = target.key.configuration,
      compilerSettings = target2Compiler[target.key] ?: continue,
      copts = info.ruleContext?.copts?.asList().orEmpty(),
      conlyopts = info.ruleContext?.conlyopts?.asList().orEmpty(),
      cxxopts = info.ruleContext?.cxxopts?.asList().orEmpty(),
      transitiveIncludes = info.compilationContext.includes,
      transitiveQuoteIncludes = info.compilationContext.quoteIncludes,
      transitiveSystemIncludes = info.compilationContext.systemIncludes,
      transitiveDefines = info.compilationContext.defines.asList(),
    )

    equivalenceClasses.putValue(configuration, target.key)
  }

  return equivalenceClasses.entrySet().mapNotNull { (clazz, targets) -> createResolveConfiguration(clazz, targets) }
}

context(ctx: CcImportContext)
private fun createResolveConfiguration(
  clazz: CcResolveConfiguration.EquivalenceClass,
  targets: Collection<WorkspaceTargetKey>,
): CcResolveConfiguration? {
  val sources = collectSources(targets)
  if (sources.isEmpty()) return null

  return CcResolveConfiguration(
    id = CcResolveConfiguration.Identifier(clazz.hashCode(), clazz.configuration),
    shared = clazz,
    targets = targets.toList(),
    sources = sources,
    name = computeDisplayName(targets),
  )
}

context(ctx: CcImportContext)
private fun collectSources(targets: Collection<WorkspaceTargetKey>): List<VirtualFileUrl> {
  return targets.asSequence()
    .mapNotNull { ctx.snapshot.targets.findTargetByKey(it, TargetLoadOptions.ALL) }
    .flatMap { target -> target.sources.getOutputLocations() }
    .mapNotNull { ctx.outputResolver.resolve(it, localOverride = ctx.snapshot.repoMapping.getLocalRepositories()) }
    .filter { OCFileTypeHelpers.isSourceFile(it.fileName.toString()) }
    .map { it.toVirtualFileUrl(ctx.vfuManager) }
    .toList()
}

context(ctx: CcImportContext)
private fun computeDisplayName(targets: Collection<WorkspaceTargetKey>): String {
  val minTargetKey = targets.minBy { it.label }

  return buildString {
    append(minTargetKey.label)
    minTargetKey.configuration.shortChecksum?.let { checksum ->
      append(" (")
      ctx.snapshot.configurations[minTargetKey.configuration]?.summary?.mnemonic?.takeIf { it.isNotBlank() }?.let {
        append(it).append(' ')
      }
      append(checksum).append(')')
    }

    if (targets.size > 1) {
      append(BazelCLionCommonBundle.message("cc.resolve.configuration.other.targets", targets.size - 1))
    }
  }
}

@ApiStatus.Internal
fun CcResolveConfiguration.Identifier.encode(): String {
  return "$CC_CONFIGURATION_MARKER:$hashCode:${configurationId.shortChecksum}"
}

fun OCResolveConfiguration.getCcIdentifier(): CcResolveConfiguration.Identifier? {
  val parts = uniqueId.split(':')
  if (parts.size != 3) return null

  val (marker, hashCode, configurationId) = parts
  if (marker != CC_CONFIGURATION_MARKER) return null

  return CcResolveConfiguration.Identifier(
    hashCode = hashCode.toIntOrNull() ?: return null,
    configurationId = WorkspaceConfigurationId.of(configurationId),
  )
}
