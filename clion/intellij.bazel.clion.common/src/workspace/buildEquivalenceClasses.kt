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
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import org.jetbrains.bsp.protocol.isGenerated
import org.jetbrains.bazel.utils.toRealPathOrNull
import java.nio.file.Path
import kotlin.io.path.isSymbolicLink
import kotlin.collections.orEmpty

private const val CC_CONFIGURATION_MARKER = "BAZEL_CC"

data class CcResolveConfiguration(
  val id: Identifier,
  val name: @NlsSafe String,
  val shared: EquivalenceClass,
  val targets: List<WorkspaceTargetKey>,

  /** Compiled sources of the targets, i.e. all files sources file in srcs. */
  val sources: List<VirtualFileUrl>,

  /** Available headers of the targets, i.e. all header files in hdrs and srcs. */
  val headers: List<VirtualFileUrl>,
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
  val buildTargets = targets.mapNotNull { ctx.snapshot.targets.findTargetByKey(it) }

  val sources = collectSources(buildTargets)
  // a file in srcs and in hdrs is compiled, so it is a source
  val headers = collectHeaders(buildTargets) - sources.toSet()

  if (sources.isEmpty() && headers.isEmpty()) return null

  return CcResolveConfiguration(
    id = CcResolveConfiguration.Identifier(clazz.hashCode(), clazz.configuration),
    shared = clazz,
    targets = targets.toList(),
    sources = sources,
    headers = headers,
    name = computeDisplayName(targets),
  )
}

/** Only actual source files in srcs are compiled by rules_cc. */
context(ctx: CcImportContext)
private fun collectSources(targets: List<BuildTarget>): List<VirtualFileUrl> {
  return targets.asSequence()
    .flatMap { target -> target.sources.getOutputLocations() }
    .resolveFiles(OCFileTypeHelpers::isSourceFile)
    .toList()
}

/** All headers in srcs and hdrs are available for compilation of the sources. */
context(ctx: CcImportContext)
private fun collectHeaders(targets: List<BuildTarget>): List<VirtualFileUrl> {
  val privateHeaders = targets.asSequence()
    .flatMap { target -> target.sources.getOutputLocations() }
    .resolveFiles(OCFileTypeHelpers::isHeaderFile)

  val directHeaders = targets.asSequence()
    .flatMap { target -> target.directHeaders() }
    .resolveFiles(
      filter = { name -> OCFileTypeHelpers.isSourceFile(name) || OCFileTypeHelpers.isHeaderFile(name) },
      collapseReExports = true,
    )

  // a header in both srcs and hdrs reaches both sequences
  return (privateHeaders + directHeaders).distinct().toList()
}

/** [collapseReExports] replaces a header that `strip_include_prefix` re-exports with its source file. */
context(ctx: CcImportContext)
private fun Sequence<OutputLocation>.resolveFiles(
  filter: (String) -> Boolean,
  collapseReExports: Boolean = false,
): Sequence<VirtualFileUrl> {
  return mapNotNull { location ->
      ctx.outputResolver.resolve(location, localOverride = ctx.snapshot.repoMapping.getLocalRepositories())
        ?.let { location to it }
    }
    .filter { (_, path) -> filter(path.fileName.toString()) }
    .map { (location, path) -> if (collapseReExports && location.isGenerated) path.originalFile() else path }
    .distinct()
    .map { it.toVirtualFileUrl(ctx.vfuManager) }
}

/** Returns the source file of a symlinked output, which the sync creates for CPP-52156. */
private fun Path.originalFile(): Path = if (isSymbolicLink()) toRealPathOrNull() ?: this else this

private fun BuildTarget.directHeaders(): Sequence<OutputLocation> {
  val compilationContext = extractData<CcBuildTarget>()?.compilationContext ?: return emptySequence()
  return compilationContext.directHeaders.getOutputLocations()
}

context(ctx: CcImportContext)
private fun computeDisplayName(targets: Collection<WorkspaceTargetKey>): String {
  val minTargetKey = targets.minBy { it.label }

  return buildString {
    append(minTargetKey.displayName(ctx.snapshot.configurations))

    if (targets.size > 1) {
      append(BazelCLionCommonBundle.message("cc.resolve.configuration.other.targets", targets.size - 1))
    }
  }
}

internal fun WorkspaceTargetKey.displayName(configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>): String {
  return buildString {
    append(label)
    configuration.shortChecksum?.let { checksum ->
      append(" (")
      configurations[configuration]?.summary?.mnemonic?.takeIf { it.isNotBlank() }?.let {
        append(it).append(' ')
      }
      append(checksum).append(')')
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
