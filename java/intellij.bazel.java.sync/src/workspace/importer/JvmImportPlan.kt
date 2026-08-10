package org.jetbrains.bazel.workspace.importer

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.isJvmTarget
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContextBuilder
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetMerger
import org.jetbrains.bazel.sync.workspace.snapshot.allSources
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.isTestTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path

@ApiStatus.Internal
val JVM_NAME_PRODUCER: NameProducer = NameProducer(id = "jvm")

@ApiStatus.Internal
class JvmImportPlan(
  rawTargets: Collection<BuildTarget>,
  jvmResolved: Map<WorkspaceTargetKey, JvmResolvedTarget>,
) {
  // merge aspect-only duplicates so the whole pipeline sees one target per (label, configuration);
  // `mergeByTargetKey` strips the aspect ids, so every key below is already aspect-free
  val targets: List<BuildTarget> = WorkspaceTargetMerger(mergeFunctions = jvmTargetMergeFunctions).mergeByTargetKey(rawTargets)

  private val allLibraries: List<LibraryItem> = jvmResolved.values.flatMap { it.libraries }.distinctBy { it.key }

  private val jarToSourceModule: Map<Path, WorkspaceTargetKey> =
    targets.asSequence()

      // build `jar -> source module` map
      .filter { it.allSources.any { path -> path.hasJvmSourceExtension() } }
      .flatMap { target ->
        target.findBuildData<JvmBuildTarget>()?.let { (it.binaryOutputs.getFiles() + it.outputInterfaceJars.getFiles()).toList() }.orEmpty()
          .asSequence().map { it to target.key }
      }
      .groupBy({ it.first }, { it.second })

      // keep only single `jar -> source module` mapping
      .mapNotNull { (jar, owners) -> owners.distinct().singleOrNull()?.let { jar to it } }
      .toMap()

  val libraryShadowsModule: Map<WorkspaceTargetKey, WorkspaceTargetKey> =
    allLibraries.asSequence()
      .mapNotNull { lib ->
        val producer = lib.jars.firstNotNullOfOrNull { jarToSourceModule[it] } ?: return@mapNotNull null
        // don't shadow own module generated, annotation processor generated code
        // is just self-referential library, so ignore it immediately
        if (producer.copy(aspectIds = WorkspaceAspectIds.EMPTY)
          == lib.key.copy(aspectIds = WorkspaceAspectIds.EMPTY)) {
          return@mapNotNull null
        }
        lib.key to producer
      }
      .toMap()

  val libraries: List<LibraryItem> = allLibraries.filterNot { it.key in libraryShadowsModule }

  val moduleKeys: List<WorkspaceTargetKey> = targets.asSequence()
    .filter { it.kind.isJvmTarget() }
    .map { it.key }
    .distinct()
    .toList()

  val libraryKeys: List<WorkspaceTargetKey> = libraries.map { it.key }.distinct()

  private val preferredModuleNames: Map<WorkspaceTargetKey, String> = targets.asSequence()
    .mapNotNull { target -> target.key to (target.preferredModuleName() ?: return@mapNotNull null) }
    .toMap()

  fun declareNames(naming: GlobalNamingContextBuilder) {
    for (key in moduleKeys) {
      naming.declare(NameSpace.MODULE, JVM_NAME_PRODUCER, key, preferred = preferredModuleNames[key])
    }
    for (key in libraryKeys) {
      naming.declare(NameSpace.LIBRARY, JVM_NAME_PRODUCER, key)
    }
  }

  private fun BuildTarget.preferredModuleName(): String? {
    // first try name hint from `BuildTargetTag.MODULE_NAME_HINT`
    // parse following tag: ide-module-name=<module-name>
    tags.singleOrNull { it.startsWith("${BuildTargetTag.MODULE_NAME_HINT}=") }
      ?.substringAfter('=')
      ?.takeIf { it.isNotBlank() }
      ?.let { return it }

    // rules_kotlin `moduleName` attribute, but only when the BUILD file sets it explicitly
    val kotlinTarget = this.findBuildData<KotlinBuildTarget>()
    val moduleName = kotlinTarget?.moduleName?.takeIf { name ->
      name.isNotBlank() &&
      !key.label.isRulesKotlinDerivedModuleName(name) &&
      // a target with `associates` compiles into the associate module, so it reports the associate name
      kotlinTarget.associates.none { it.label.isRulesKotlinDerivedModuleName(name) }
    }
    if (moduleName != null) {
      // TODO: remove test classification naming after fixing jps-to-bazel to not duplicate `moduleName`
      return if (this.isTestTarget()) "$moduleName-test" else moduleName
    }

    // fallback to default module name resolver
    return null
  }
}

// rules_kotlin always provide `moduleName` regardless of user setting explicitly or not
// we only want to use `moduleName` as module naming hint when it's NOT the rules_kotlin provided one
// implemented based on `kotlin/internal/utils/utils.bzl`
private fun Label.isRulesKotlinDerivedModuleName(moduleName: String): Boolean {
  val packagePart = packagePath.pathSegments.joinToString(separator = "_")
  val targetPart = targetName.replace('/', '_')
  return moduleName == "$packagePart-$targetPart" || (packagePart.isEmpty() && moduleName == targetPart)
}
