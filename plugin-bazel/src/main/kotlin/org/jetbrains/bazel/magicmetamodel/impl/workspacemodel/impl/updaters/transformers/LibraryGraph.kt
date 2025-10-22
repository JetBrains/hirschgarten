package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_MODULE_ENTITY_TYPE_ID
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

class LibraryGraph(private val libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(
    target: RawBuildTarget,
    includesTransitive: Boolean = !BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled,
  ): List<DependencyLabel> =
    if (includesTransitive) {
      calculateAllTransitiveDependencies(target)
    } else {
      calculateDirectDependencies(target)
    }

  private fun calculateAllTransitiveDependencies(target: RawBuildTarget): List<DependencyLabel> {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf(target.id)
    val result = mutableListOf<DependencyLabel>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode.label !in visited) {
        // don't traverse further when hitting modules
        if (currentNode.label.isCurrentNodeLibrary()) {
          toVisit += graph[currentNode.label].orEmpty()
        }
        visited += currentNode.label
        result += currentNode
      }
    }
    return result
  }

  private fun calculateDirectDependencies(target: RawBuildTarget): List<DependencyLabel> = target.dependencies

  private fun Label.isCurrentNodeLibrary() = this in graph

  fun createLibraries(project: Project): List<Library> =
    libraries
      .map {
        Library(
          displayName = it.id.formatAsModuleName(project),
          iJars = it.ijars,
          classJars = it.jars,
          sourceJars = it.sourceJars,
          mavenCoordinates = it.mavenCoordinates,
          isLowPriority = it.isLowPriority,
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
              type = JAVA_MODULE_ENTITY_TYPE_ID,
              dependencies =
                listOf(libraryName) +
                  library.dependencies.map { dep -> dep.toDependencyId(project) },
              kind =
                TargetKind(
                  kindString = "java_library",
                  ruleType = RuleType.LIBRARY,
                  languageClasses = setOf(LanguageClass.JAVA),
                ),
              isLibraryModule = true,
            ),
          jvmJdkName = defaultJdkName,
          baseDirContentRoot = null,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          runtimeDependencies = library.dependencies.filter { it.isRuntime }.map { it.toDependencyId(project) },
        )
      }
  }

  private fun Label.isLibraryId() = this in graph.keys

  private fun DependencyLabel.toDependencyId(project: Project): String {
    val rawId = label.formatAsModuleName(project)
    return if (label.isLibraryId()) rawId.addLibraryModulePrefix() else rawId
  }
}
