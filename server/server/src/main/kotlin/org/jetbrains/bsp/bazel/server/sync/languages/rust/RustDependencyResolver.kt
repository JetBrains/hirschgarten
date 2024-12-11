package org.jetbrains.bsp.bazel.server.sync.languages.rust

import ch.epfl.scala.bsp4j.RustDepKind
import ch.epfl.scala.bsp4j.RustDepKindInfo
import ch.epfl.scala.bsp4j.RustDependency
import ch.epfl.scala.bsp4j.RustPackage
import ch.epfl.scala.bsp4j.RustRawDependency
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Module

data class RustDependencies(val dependencies: Map<String, List<RustDependency>>, val rawDependencies: Map<String, List<RustRawDependency>>)

class RustDependencyResolver {
  // We need to resolve all dependencies and provide a list of new bazel targets
  // to be transformed into packages.
  fun rustDependencies(rustPackages: List<RustPackage>, rustBspTargets: List<Module>): RustDependencies {
    val associatedBspTargets = groupBspTargetsByPackage(rustBspTargets, rustPackages)
    val associatedRawBspTargets = groupBspRawTargetsByPackage(rustBspTargets, rustPackages)

    val rustDependencies = resolveDependencies(associatedBspTargets)
    val rustRawDependencies = resolveRawDependencies(associatedRawBspTargets)

    return RustDependencies(rustDependencies, rustRawDependencies)
  }

  private fun groupBspTargetsByPackage(rustBspTargets: List<Module>, rustPackages: List<RustPackage>): Map<RustPackage, List<Module>> {
    val rustBspTargetsMappedToLabel = rustBspTargets.associateBy { it.label }
    return rustPackages.associateWith {
      it.resolvedTargets.mapNotNull { pkgTarget ->
        rustBspTargetsMappedToLabel[Label.parse("${it.id}:${pkgTarget.name}")]
      }
    }
  }

  private fun groupBspRawTargetsByPackage(rustBspTargets: List<Module>, rustPackages: List<RustPackage>): Map<RustPackage, List<Module>> =
    rustPackages.associateWith { pkg ->
      rustBspTargets.filter { it.label.packagePath == pkg.id }
    }

  private fun resolveDependencies(associatedBspTargets: Map<RustPackage, List<Module>>): Map<String, List<RustDependency>> =
    resolveRustDependencies(associatedBspTargets, ::resolveBspDependencies)

  private fun resolveBspDependencies(rustPackage: RustPackage, directDependencies: List<Label>): Pair<String, List<RustDependency>> {
    val dependencies =
      directDependencies
        .map(::createDependency)
        .filter { rustPackage.id != it.pkg }
    return Pair(rustPackage.id, dependencies)
  }

  private fun createDependency(bazelPackageTargetInfo: Label): RustDependency {
    val dep = RustDependency(bazelPackageTargetInfo.packagePath)
    dep.name = bazelPackageTargetInfo.targetName
    dep.depKinds = listOf(RustDepKindInfo(RustDepKind.NORMAL))
    return dep
  }

  private fun resolveRawDependencies(associatedRawBspTargets: Map<RustPackage, List<Module>>): Map<String, List<RustRawDependency>> =
    resolveRustDependencies(associatedRawBspTargets, ::resolveRawBspDependencies)

  private fun resolveRawBspDependencies(rustPackage: RustPackage, directDependencies: List<Label>): Pair<String, List<RustRawDependency>> =
    Pair(
      rustPackage.id,
      directDependencies.map {
        RustRawDependency(
          it.toString(),
          false,
          true,
          setOf<String>(),
        )
      },
    )

  private fun <T> resolveRustDependencies(
    associatedBspTargets: Map<RustPackage, List<Module>>,
    resolver: (RustPackage, List<Label>) -> Pair<String, List<T>>,
  ): Map<String, List<T>> =
    associatedBspTargets
      .flatMap { resolvePackageDependencies(it.key, it.value, resolver) }
      .toMap()
      .filter { (_, value) -> value.isNotEmpty() }

  private fun <T> resolvePackageDependencies(
    rustPackage: RustPackage,
    bspTargets: List<Module>,
    resolver: (RustPackage, List<Label>) -> Pair<String, List<T>>,
  ): List<Pair<String, List<T>>> =
    bspTargets.map { bspTarget ->
      resolver(rustPackage, bspTarget.directDependencies)
    }
}
