package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_MODULE_ENTITY_TYPE_ID
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

class LibraryGraph(private val libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(
    target: RawBuildTarget,
  ): List<DependencyLabel> =
    calculateDirectDependencies(target)

  private fun calculateDirectDependencies(target: RawBuildTarget): List<DependencyLabel> = target.dependencies

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
                listOf(Dependency(libraryName, isRuntimeOnly = false, exported = true)) +
                library.dependencies.map { dep ->
                  Dependency(
                    dep.toDependencyId(project),
                    isRuntimeOnly = dep.isRuntime,
                    exported = dep.exported,
                  )
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

  private fun DependencyLabel.toDependencyId(project: Project): String {
    val rawId = label.formatAsModuleName(project)
    return if (label.isLibraryId()) rawId.addLibraryModulePrefix() else rawId
  }
}
