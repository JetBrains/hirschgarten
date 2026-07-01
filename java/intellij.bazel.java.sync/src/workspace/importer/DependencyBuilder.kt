package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.roots.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractJvmBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import com.intellij.platform.workspace.jps.entities.DependencyScope as EntitiesDependencyScope

// RC: those were present in original MMM
private val dependencyInterner: Interner<ModuleDependencyItem> = Interner.createWeakInterner()
private val idInterner: Interner<SymbolicEntityId<*>> = Interner.createWeakInterner()

/**
 * Resolves per-target JVM dependencies (including the strict-dependency export closure).
 *
 * Intended construction lifecycle: one [DependencyBuilder] per import pass, shared across all
 * per-target builds so the strict-dependency closure is memoized once.
 */
// RC: replaces the dependency-resolution and exported-closure slice of `ProjectDetailsToModuleDetailsTransformer`
// (incl. its `DependenciesClosure` memo), plus the `Dependency` wrapper
@ApiStatus.Internal
class DependencyBuilder(
  private val targets: Collection<RawBuildTarget>,
  private val libraryShadowsModule: Map<WorkspaceTargetKey, WorkspaceTargetKey> = emptyMap(),
) {
  private val targetsIndex: Map<WorkspaceTargetKey, RawBuildTarget> = targets.associateBy { it.key.stripAspects() }
  private val strictDependencies: Map<WorkspaceTargetKey, List<Label>> = calculateExportedDependenciesTransitiveClosure()

  data class Resolved(
    val dependencies: List<DependencyLabel>,
    val strictDependenciesCheck: StrictDependencyCheckedType,
    val strictDependencies: List<Label>,
  )

  fun resolve(target: RawBuildTarget): Resolved {
    val jvmBuildTarget = extractJvmBuildTarget(target)
    val deps = jvmBuildTarget?.jvmDependencies?.map { dep ->
      // TODO: investigate "experimental_prune_transitive_deps" option in .bazelrc
      when (dep) {
        is JvmDependency.ModuleDependency ->
          // TODO: Ultimate monorepo specifics. Drop shortly
          if (target.kind.kind == "jvm_library" || target.kind.kind == "_jvm_library_jps")
            dep.dependency
          else dep.dependency.export()

        is JvmDependency.LibraryDependency ->
          redirectLibraryShadow(target.key, dep.dependency)?.export() ?: dep.dependency.export()
      }
    }?.distinct() ?: target.dependencies
    return Resolved(
      dependencies = deps,
      strictDependenciesCheck = checkStrictDependencies(target),
      strictDependencies = strictDependencies[target.key.stripAspects()] ?: emptyList(),
    )
  }

  private fun checkStrictDependencies(target: RawBuildTarget): StrictDependencyCheckedType {
    if (!target.isWorkspace) return StrictDependencyCheckedType.OFF
    return extractJvmBuildTarget(target)?.checkStrictDependencies ?: StrictDependencyCheckedType.OFF
  }

  private fun calculateExportedDependenciesTransitiveClosure(): Map<WorkspaceTargetKey, List<Label>> {
    val strictTargets = targets.filter { checkStrictDependencies(it) != StrictDependencyCheckedType.OFF }.map { it.key.stripAspects() }

    val fDependencies: ((WorkspaceTargetKey) -> List<DependencyLabel>) = dependencies@{ key ->
      val jvmTarget = targetsIndex[key]?.let { extractJvmBuildTarget(it) } ?: return@dependencies emptyList()
      jvmTarget.jvmDependencies.map { redirectLibraryShadow(key, it.dependency) ?: it.dependency }
        .filter { it.targetKey.label != key.label /* filter out module dependency on library */ }
        .distinct()
    }

    val exportedDependenciesClosure = DependenciesClosure { key ->
      fDependencies(key).filter { it.exported }.map { it.targetKey.stripAspects() }
    }

    return strictTargets.associateWith { targetKey ->
      fDependencies(targetKey)
        .flatMap { exportedDependenciesClosure[it.targetKey.stripAspects()].map { key -> key.label } + it.targetKey.label }
        .distinct()
    }
  }

  private class DependenciesClosure(val deps: (WorkspaceTargetKey) -> List<WorkspaceTargetKey>) {
    private val cache = HashMap<WorkspaceTargetKey, Set<WorkspaceTargetKey>>()
    operator fun get(key: WorkspaceTargetKey): Set<WorkspaceTargetKey> =
      cache.getOrPut(key) {
        val ds = deps(key)
        ds.toSet() + ds.flatMap { get(it) }
      }
  }

  private fun redirectLibraryShadow(consumer: WorkspaceTargetKey, dep: DependencyLabel): DependencyLabel? {
    val producer = libraryShadowsModule[dep.targetKey] ?: return null
    if (producer.stripAspects() == consumer.stripAspects()) {
      return null
    }
    return DependencyLabel(targetKey = producer)
  }
}

private fun DependencyLabel.export(): DependencyLabel =
  if (this.kind == DependencyLabelKind.COMPILE) this.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME) else this

private fun WorkspaceTargetKey.stripAspects(): WorkspaceTargetKey = copy(aspectIds = WorkspaceAspectIds.EMPTY)

internal fun toLibraryDependency(
  libraryName: String,
  exported: Boolean,
  scope: DependencyScope = DependencyScope.COMPILE,
): LibraryDependency =
  dependencyInterner.intern(
    LibraryDependency(
      library =
        idInterner.intern(
          LibraryId(
            name = libraryName,
            tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
          ),
        ) as LibraryId,
      exported = exported,
      scope = scope.toEntityDependencyScope(),
    ),
  ) as LibraryDependency

private fun DependencyScope.toEntityDependencyScope(): EntitiesDependencyScope = when (this) {
  DependencyScope.COMPILE -> EntitiesDependencyScope.COMPILE
  DependencyScope.RUNTIME -> EntitiesDependencyScope.RUNTIME
  DependencyScope.PROVIDED -> EntitiesDependencyScope.PROVIDED
  DependencyScope.TEST -> EntitiesDependencyScope.TEST
}
