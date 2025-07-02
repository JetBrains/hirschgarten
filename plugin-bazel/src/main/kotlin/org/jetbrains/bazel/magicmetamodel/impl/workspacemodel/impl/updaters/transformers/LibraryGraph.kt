package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class LibraryGraphDependencies(val libraryDependencies: Set<Label>, val moduleDependencies: Set<Label>)

class LibraryGraph(private val libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(
    target: RawBuildTarget,
    includesTransitive: Boolean = !BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled,
  ): LibraryGraphDependencies =
    if (includesTransitive) {
      calculateAllTransitiveDependencies(target)
    } else {
      calculateDirectDependencies(target)
    }

  private fun calculateAllTransitiveDependencies(target: RawBuildTarget): LibraryGraphDependencies {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf<Label>(target.id)

    val resultLibraries = mutableSetOf<Label>()
    val resultModules = mutableSetOf<Label>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode !in visited) {
        // don't traverse further when hitting modules
        if (currentNode.isCurrentNodeLibrary()) {
          toVisit += graph[currentNode].orEmpty()
        }
        visited += currentNode

        currentNode.addToCorrectResultSet(resultLibraries, resultModules)
      }
    }

    return LibraryGraphDependencies(
      libraryDependencies = resultLibraries,
      moduleDependencies = resultModules,
    )
  }

  private fun calculateDirectDependencies(target: RawBuildTarget): LibraryGraphDependencies {
    val (libraryDependencies, moduleDependencies) =
      target.dependencies.partition { it.isCurrentNodeLibrary() }
    return LibraryGraphDependencies(
      libraryDependencies = libraryDependencies.toSet(),
      moduleDependencies = moduleDependencies.toSet(),
    )
  }

  private fun Label.isCurrentNodeLibrary() = this in graph

  private fun Label.addToCorrectResultSet(resultLibraries: MutableSet<Label>, resultModules: MutableSet<Label>) {
    if (isCurrentNodeLibrary()) {
      resultLibraries += this
    } else {
      resultModules += this
    }
  }

  fun createLibraries(project: Project): List<Library> =
    libraries
      .map {
        Library(
          displayName = it.id.formatAsModuleName(project),
          iJars = it.ijars,
          classJars = it.jars,
          sourceJars = it.sourceJars,
          mavenCoordinates = it.mavenCoordinates,
        )
      }

  fun createLibraryModules(project: Project, defaultJdkName: String?): List<JavaModule> {
    if (!BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled) return emptyList()

    return libraries
      .map { library ->
        val libraryName = library.id.formatAsModuleName(project)
        val libraryModuleName = libraryName.addLibraryModulePrefix()
        JavaModule(
          genericModuleInfo =
            GenericModuleInfo(
              name = libraryModuleName,
              type = ModuleTypeId(StdModuleTypes.JAVA.id),
              librariesDependencies = listOf(IntermediateLibraryDependency(libraryName, true)),
              kind =
                TargetKind(
                  kindString = "java_library",
                  ruleType = RuleType.LIBRARY,
                  languageClasses = setOf(LanguageClass.JAVA),
                ),
              modulesDependencies =
                library.dependencies.map { targetId ->
                  val rawId = targetId.formatAsModuleName(project)
                  val id = if (targetId.isLibraryId()) rawId.addLibraryModulePrefix() else rawId
                  IntermediateModuleDependency(id)
                },
              isLibraryModule = true,
            ),
          jvmJdkName = defaultJdkName,
          baseDirContentRoot = null,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          compiledClassesPath = library.jars.firstOrNull() // usually dependency contains only a single JAR archive
        )
      }
  }

  private fun Label.isLibraryId() = this in graph.keys
}
