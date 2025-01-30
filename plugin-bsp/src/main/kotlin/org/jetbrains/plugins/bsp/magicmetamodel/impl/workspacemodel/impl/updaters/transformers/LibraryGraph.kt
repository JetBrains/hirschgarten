package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bsp.protocol.Dependency
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.target.addLibraryModulePrefix
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.Library

data class LibraryGraphDependencies(val libraryDependencies: Set<Dependency>, val moduleDependencies: Set<Dependency>)

class LibraryGraph(
  private val libraries: List<LibraryItem>,
  private val includesTransitive: Boolean = !BspFeatureFlags.isWrapLibrariesInsideModulesEnabled,
) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(target: BuildTarget, dependenciesExported: List<Boolean>?): LibraryGraphDependencies =
    if (includesTransitive) {
      calculateAllTransitiveDependencies(target)
    } else {
      calculateDirectDependencies(target, dependenciesExported)
    }

  private fun calculateAllTransitiveDependencies(target: BuildTarget): LibraryGraphDependencies {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf<BuildTargetIdentifier>(target.id)

    val resultLibraries = mutableSetOf<BuildTargetIdentifier>()
    val resultModules = mutableSetOf<BuildTargetIdentifier>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode !in visited) {
        // don't traverse further when hitting modules
        if (currentNode.isLibrary()) {
          toVisit += graph[currentNode].orEmpty().map { dep -> dep.id }
        }
        visited += currentNode

        currentNode.addToCorrectResultSet(resultLibraries, resultModules)
      }
    }

    // TODO: support exported flags if isWrapLibrariesInsideModulesEnabled == false.
    //  Or just wait until IDEA 2025.1 when K2 is default and isWrapLibrariesInsideModulesEnabled is removed (together with this function).
    return LibraryGraphDependencies(
      libraryDependencies = resultLibraries.map { id -> Dependency(id, exported = true) }.toSet(),
      moduleDependencies = resultModules.map { id -> Dependency(id, exported = true) }.toSet(),
    )
  }

  private fun calculateDirectDependencies(target: BuildTarget, dependenciesExported: List<Boolean>?): LibraryGraphDependencies {
    val dependencies =
      if (dependenciesExported == null) {
        target.dependencies.map { id -> Dependency(id, exported = true) }
      } else {
        target.dependencies.zip(dependenciesExported).map { (id, exported) -> Dependency(id, exported) }
      }

    val (libraryDependencies, moduleDependencies) =
      dependencies.partition { it.id.isLibrary() }
    return LibraryGraphDependencies(
      libraryDependencies = libraryDependencies.toSet(),
      moduleDependencies = moduleDependencies.toSet(),
    )
  }

  private fun BuildTargetIdentifier.isLibrary() = this in graph

  private fun BuildTargetIdentifier.addToCorrectResultSet(
    resultLibraries: MutableSet<BuildTargetIdentifier>,
    resultModules: MutableSet<BuildTargetIdentifier>,
  ) {
    if (isLibrary()) {
      resultLibraries += this
    } else {
      resultModules += this
    }
  }

  fun createLibraries(nameProvider: TargetNameReformatProvider): List<Library> =
    libraries
      .map {
        Library(
          displayName = nameProvider(BuildTargetInfo(id = it.id)),
          iJars = it.ijars,
          classJars = it.jars,
          sourceJars = it.sourceJars,
          mavenCoordinates = it.mavenCoordinates,
        )
      }

  fun createLibraryModules(nameProvider: TargetNameReformatProvider, defaultJdkName: String?): List<JavaModule> {
    if (!BspFeatureFlags.isWrapLibrariesInsideModulesEnabled) return emptyList()

    return libraries
      .map { library ->
        val libraryName = nameProvider(BuildTargetInfo(id = library.id))
        val libraryModuleName = libraryName.addLibraryModulePrefix()
        JavaModule(
          genericModuleInfo =
            GenericModuleInfo(
              name = libraryModuleName,
              type = ModuleTypeId(StdModuleTypes.JAVA.id),
              librariesDependencies = listOf(IntermediateLibraryDependency(libraryName, isProjectLevelLibrary = true, exported = true)),
              modulesDependencies =
                library.dependencies.map { dep ->
                  val rawId = nameProvider(BuildTargetInfo(id = dep.id))
                  val id = if (dep.id.isLibraryId()) rawId.addLibraryModulePrefix() else rawId
                  IntermediateModuleDependency(id, exported = dep.exported)
                },
              isLibraryModule = true,
              languageIds = listOf("java"),
            ),
          jvmJdkName = defaultJdkName,
          baseDirContentRoot = null,
          moduleLevelLibraries = null,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
        )
      }
  }

  private fun BuildTargetIdentifier.isLibraryId() = this in graph.keys
}
