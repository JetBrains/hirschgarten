package org.jetbrains.bazel.server.sync

import ch.epfl.scala.bsp4j.MavenDependencyModule
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Library
import org.jetbrains.bazel.server.model.Module

object DependencyMapper {
  fun extractMavenDependencyInfo(lib: Library): MavenDependencyModule? {
    if (lib.outputs.isEmpty()) return null
    val mavenCoordinates = lib.mavenCoordinates ?: return null
    val jars =
      lib.outputs.map { uri -> uri.toString() }.map {
        MavenDependencyModuleArtifact(it)
      }
    val sourceJars =
      lib.sources.map { uri -> uri.toString() }.map {
        val artifact = MavenDependencyModuleArtifact(it)
        artifact.classifier = "sources"
        artifact
      }
    return MavenDependencyModule(mavenCoordinates.groupId, mavenCoordinates.artifactId, mavenCoordinates.version, jars + sourceJars)
  }

  fun allModuleDependencies(project: AspectSyncProject, module: Module): HashSet<Library> {
    val toResolve = mutableListOf<Label>()
    toResolve.addAll(module.directDependencies)
    val accumulator = HashSet<Library>()
    val allSeenTargets = toResolve.toMutableSet()
    while (toResolve.isNotEmpty()) {
      val lib = project.libraries[toResolve.removeLast()]
      if (lib != null && !accumulator.contains(lib)) {
        accumulator.add(lib)
        allSeenTargets.add(lib.label)
        for (dep in lib.dependencies) {
          if (allSeenTargets.add(dep)) {
            toResolve.add(dep)
          }
        }
      }
    }
    return accumulator
  }
}
