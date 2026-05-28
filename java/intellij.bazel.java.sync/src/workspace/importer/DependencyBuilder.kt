package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.roots.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.JvmDependency
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
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
@ApiStatus.Internal
class DependencyBuilder(private val targets: Collection<RawBuildTarget>) {
  private val targetsIndex: Map<Label, RawBuildTarget> = targets.associateBy { it.id }
  private val strictDependencies: Map<Label, List<Label>> = calculateExportedDependenciesTransitiveClosure()

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
          dep.dependency.export()
      }
    } ?: target.dependencies
    return Resolved(
      dependencies = deps,
      strictDependenciesCheck = checkStrictDependencies(target),
      strictDependencies = strictDependencies[target.id] ?: emptyList(),
    )
  }

  /**
   * Converts dependency labels to workspace-model [ModuleDependencyItem]s, respecting the
   * monorepo-style preference for module-over-library when both exist with the same id.
   */
  fun toModuleDependencyItems(
    moduleName: String,
    resolved: Resolved,
    repoMapping: RepoMapping,
    knownModuleNames: Set<String>,
    knownLibraryNames: Set<String>,
    associates: List<String>,
    defaultDependencies: List<ModuleDependencyItem>,
  ): List<ModuleDependencyItem> {
    val dependenciesFromEntity = resolved.dependencies.mapNotNull { dep ->
      val depModuleName = dep.label.formatAsModuleName(repoMapping)
      val scope = if (dep.isRuntime) DependencyScope.RUNTIME else DependencyScope.COMPILE
      // Libraries may have the same name as their container module.
      // Prefer module dependency and use the library only inside the containing module to prevent dependency loops.
      val asModule = depModuleName.takeIf { it in knownModuleNames && it != moduleName }
      if (asModule != null) {
        return@mapNotNull toModuleDependency(asModule, exported = dep.exported, scope = scope)
      }
      val asLibrary = depModuleName.takeIf { it in knownLibraryNames }
      if (asLibrary != null) {
        return@mapNotNull toLibraryDependency(asLibrary, exported = dep.exported, scope = scope)
      }
      null
    }
    val associatesDependencies = associates.map { toModuleDependency(it, exported = true) }
    return defaultDependencies + dependenciesFromEntity + associatesDependencies
  }

  private fun checkStrictDependencies(target: RawBuildTarget): StrictDependencyCheckedType {
    if (!target.isWorkspace) return StrictDependencyCheckedType.OFF
    return extractJvmBuildTarget(target)?.checkStrictDependencies ?: StrictDependencyCheckedType.OFF
  }

  private fun calculateExportedDependenciesTransitiveClosure(): Map<Label, List<Label>> {
    val strictTargets = targets.filter { checkStrictDependencies(it) != StrictDependencyCheckedType.OFF }.map { it.id }

    val fDependencies: ((Label) -> List<DependencyLabel>) = dependencies@ { label ->
      val jvmTarget = targetsIndex[label]?.let { extractJvmBuildTarget(it) } ?: return@dependencies emptyList()
      jvmTarget.jvmDependencies.map { it.dependency }
        .filter { it.label != label /* filter out module dependency on library */ }
    }

    val exportedDependenciesClosure = DependenciesClosure { label ->
      fDependencies(label).filter { it.exported }.map { it.label }
    }

    return strictTargets.associateWith { targetId ->
      fDependencies(targetId).flatMap { exportedDependenciesClosure[it.label] + it.label }.distinct()
    }
  }

  private class DependenciesClosure(val deps: (Label) -> List<Label>) {
    private val cache = HashMap<Label, Set<Label>>()
    operator fun get(label: Label): Set<Label> =
      cache.getOrPut(label) {
        val ds = deps(label)
        ds.toSet() + ds.flatMap { get(it) }
      }
  }
}

private fun DependencyLabel.export(): DependencyLabel =
  if (this.kind == DependencyLabelKind.COMPILE) this.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME) else this

private fun toModuleDependency(
  moduleName: String,
  exported: Boolean,
  scope: DependencyScope = DependencyScope.COMPILE,
): ModuleDependency =
  dependencyInterner.intern(
    ModuleDependency(
      module = idInterner.intern(ModuleId(moduleName)) as ModuleId,
      exported = exported,
      scope = scope.toEntityDependencyScope(),
      productionOnTest = true,
    ),
  ) as ModuleDependency

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
