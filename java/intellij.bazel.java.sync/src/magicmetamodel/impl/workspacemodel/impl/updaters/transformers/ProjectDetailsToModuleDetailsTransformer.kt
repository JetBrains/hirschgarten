package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList
import org.jetbrains.bsp.protocol.JvmDependency
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget

@ApiStatus.Internal
class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails) {
  private val targetsIndex: Map<Label, RawBuildTarget> = projectDetails.targets.associateBy { it.id }
  private val strictDependencies: Map<Label, List<Label>> = calculateExportedDependenciesTransitiveClosure()

  fun moduleDetailsForTargetId(targetId: Label): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    val jvmBuildTarget = extractJvmBuildTarget(target)
    return ModuleDetails(
      target = target,
      javacOptions = jvmBuildTarget?.javacOpts ?: emptyList(),
      // TODO: update type and use jvm dependencies only
      dependencies = if (jvmBuildTarget == null) {
        target.dependencies
      } else {
        jvmBuildTarget.jvmDependencies.map { dep ->
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
        }
      },
      strictDependenciesCheck = checkStrictDependencies(target),
      strictDependencies = strictDependencies[targetId] ?: emptyList(),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = jvmBuildTarget?.binaryOutputs ?: emptyList(),
    )
  }

  private fun checkStrictDependencies(target: RawBuildTarget): StrictDependencyCheckedType {
    if (!target.isWorkspace)
      return StrictDependencyCheckedType.OFF
    return extractJvmBuildTarget(target)?.checkStrictDependencies ?: StrictDependencyCheckedType.OFF
  }

  private fun calculateExportedDependenciesTransitiveClosure(): Map<Label, List<Label>> {
    val targets = projectDetails.targets.filter { checkStrictDependencies(it) != StrictDependencyCheckedType.OFF }.map { it.id }

    val fDependencies: ((Label) -> List<DependencyLabel>) = dependencies@ { label ->
      val jvmTarget = targetsIndex[label]?.let { extractJvmBuildTarget(it) } ?: return@dependencies emptyList()
      jvmTarget.jvmDependencies.map { it.dependency }
        .filter { it.label != label /* filter out module dependency on library */ }
    }

    val exportedDependenciesClosure = DependenciesClosure { label ->
      fDependencies(label).filter { it.exported }.map { it.label }
    }

    return targets.associateWith { targetId ->
      fDependencies(targetId).flatMap { exportedDependenciesClosure[it.label] + it.label }.distinct()
    }
  }

  private class DependenciesClosure(val deps: (Label) -> List<Label>) {
    private val cache = HashMap<Label, Set<Label>>()
    operator fun get(label: Label): Set<Label> {
      return cache.getOrPut(label) {
        val deps = deps(label)
        deps.toSet() + deps.flatMap { get(it) }
      }
    }
  }

  private fun DependencyLabel.export(): DependencyLabel =
    if (this.kind == DependencyLabelKind.COMPILE) this.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME) else this
}
