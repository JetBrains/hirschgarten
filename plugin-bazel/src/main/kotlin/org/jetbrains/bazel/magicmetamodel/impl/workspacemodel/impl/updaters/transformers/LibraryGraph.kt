package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_MODULE_ENTITY_TYPE_ID
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

class LibraryGraph(private val libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(
    target: RawBuildTarget,
    includesTransitive: Boolean = !BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled,
  ): List<Label> =
    if (includesTransitive) {
      calculateAllTransitiveDependencies(target)
    } else {
      calculateDirectDependencies(target)
    }

  private fun calculateAllTransitiveDependencies(target: RawBuildTarget): List<Label> {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf(target.id)
    val result = mutableListOf<Label>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode !in visited) {
        // don't traverse further when hitting modules
        if (currentNode.isCurrentNodeLibrary()) {
          toVisit += graph[currentNode].orEmpty()
        }
        visited += currentNode
        result += currentNode
      }
    }
    return result
  }

  private fun calculateDirectDependencies(target: RawBuildTarget): List<Label> = target.dependencies

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
                  library.dependencies.map { targetId ->
                    val rawId = targetId.formatAsModuleName(project)
                    val id = if (targetId.isLibraryId()) rawId.addLibraryModulePrefix() else rawId
                    id
                  },
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
        )
      }
  }

  private fun Label.isLibraryId() = this in graph.keys
}
