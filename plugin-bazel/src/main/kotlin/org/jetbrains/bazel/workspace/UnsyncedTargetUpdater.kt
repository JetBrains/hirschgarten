package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.InheritedSdkDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.util.containers.Interner
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.defaultJdkName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.workspace.mapper.normal.TargetTagsResolver
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

// Interners for deduplicating ModuleId and ModuleDependency objects
private val moduleIdInterner: Interner<ModuleId> = Interner.createWeakInterner()
private val moduleDependencyInterner: Interner<ModuleDependencyItem> = Interner.createWeakInterner()

class UnsyncedTargetUpdater {
  companion object {
    /**
     * Fetches target information for an unsynced target via partial sync and updates the target cache.
     * Returns the fetched RawBuildTarget and dependencies, or null if the target should be ignored.
     */
    suspend fun fetchAndCacheUnsyncedTarget(
      label: Label,
      project: Project,
      snapshot: ImmutableEntityStorage,
      storage: MutableEntityStorage,
    ): Pair<RawBuildTarget, List<ModuleDependencyItem>>? {
      val dependencies = mutableListOf<ModuleDependencyItem>()

      try {
        val partialSyncResult = project.connection.runWithServer { server ->
          server.workspaceBuildTargets(
            WorkspaceBuildTargetParams(
              WorkspaceBuildTargetSelector.SpecificTargets(listOf(label)),
              TaskGroupId.EMPTY.task("unsynced-target"),
            )
          )
        }

        // Extract the target info from the partial sync result
        val rawAspectTarget = partialSyncResult.targets[label]
        if (rawAspectTarget != null) {
          val targetInfo = rawAspectTarget
          if (targetInfo.tagsList.contains("no-ide")) {
            return null
          }

          // Add SDK Dependency
          // Use InheritedSdkDependency when matching project default for true SDK inheritance
          val languages = inferLanguages(targetInfo)
          if (languages.contains(LanguageClass.JAVA)) {
            val defaultJdk = project.defaultJdkName
            if (defaultJdk != null) {
              dependencies.add(InheritedSdkDependency)
            }
          }

          // Transform the TargetInfo to RawBuildTarget and save to TargetUtils
          try {
            val targetKind = inferKind(TargetTagsResolver().resolveTags(targetInfo), targetInfo.kind, languages)
            val baseDirectory = project.rootDir.toNioPath()

            // Convert dependencies from protobuf format to Label list
            val targetDependencies = targetInfo.depsList.map { DependencyLabel(Label.parse(it.target.label)) }

            // Convert sources from protobuf format to SourceItem list
            val sources = targetInfo.sourcesList.map { fileLocation: BspTargetInfo.ArtifactLocation ->
              SourceItem(
                path = baseDirectory.resolve(fileLocation.relativePath),
                generated = !fileLocation.isSource,
                jvmPackagePrefix = null
              )
            }

            // Convert resources from protobuf format
            val resources = targetInfo.resourcesList.map { fileLocation ->
              baseDirectory.resolve(fileLocation.relativePath)
            }

            // Create a minimal RawBuildTarget from the TargetInfo
            val rawBuildTarget = RawBuildTarget(
              id = label,
              tags = targetInfo.tagsList,
              dependencies = targetDependencies,
              kind = targetKind,
              sources = sources,
              resources = resources,
              baseDirectory = baseDirectory,
              noBuild = false,
              data = null, // Will be set by language-specific processors in full sync
            )
            project.targetUtils.addTargets(mapOf(label to rawBuildTarget), project)

            // Add module dependencies
            val moduleDeps = buildModuleDependencies(
              rawBuildTarget,
              project,
              snapshot,
              storage,
            )
            dependencies.addAll(moduleDeps)

            return rawBuildTarget to dependencies
          } catch (e: Exception) {
            e.printStackTrace()
            return null
          }
        } else {
          return null
        }
      } catch (ex: Exception) {
        ex.printStackTrace()
        return null
      }
    }

    /**
     * Batch-fetches target information for multiple unsynced targets in a single RPC call.
     * Returns a map from Label to (RawBuildTarget, dependencies) for valid targets.
     * Filters out targets with "no-ide" tag.
     */
    suspend fun fetchAndCacheUnsyncedTargets(
      labels: List<Label>,
      project: Project,
      snapshot: ImmutableEntityStorage,
      storage: MutableEntityStorage,
    ): Map<Label, Pair<RawBuildTarget, List<ModuleDependencyItem>>> {
      if (labels.isEmpty()) return emptyMap()
      val result = mutableMapOf<Label, Pair<RawBuildTarget, List<ModuleDependencyItem>>>()
      try {
        val partialSyncResult = project.connection.runWithServer { server ->
          server.workspaceBuildTargets(
            WorkspaceBuildTargetParams(
              WorkspaceBuildTargetSelector.SpecificTargets(labels),
              TaskGroupId.EMPTY.task("unsynced-targets-batch"),
            )
          )
        }

        val baseDirectory = project.rootDir.toNioPath()
        for (label in labels) {
          val targetInfo = partialSyncResult.targets[label] ?: continue
          if (targetInfo.tagsList.contains("no-ide")) continue

          val dependencies = mutableListOf<ModuleDependencyItem>()
          val languages = inferLanguages(targetInfo)
          if (languages.contains(LanguageClass.JAVA)) {
            val defaultJdk = project.defaultJdkName
            if (defaultJdk != null) {
              dependencies.add(InheritedSdkDependency)
            }
          }

          try {
            val targetKind = inferKind(TargetTagsResolver().resolveTags(targetInfo), targetInfo.kind, languages)
            val targetDependencies = targetInfo.depsList.map { DependencyLabel(Label.parse(it.target.label)) }
            val sources = targetInfo.sourcesList.map { fileLocation: BspTargetInfo.ArtifactLocation ->
              SourceItem(
                path = baseDirectory.resolve(fileLocation.relativePath),
                generated = !fileLocation.isSource,
                jvmPackagePrefix = null
              )
            }
            val resources = targetInfo.resourcesList.map { fileLocation ->
              baseDirectory.resolve(fileLocation.relativePath)
            }

            val rawBuildTarget = RawBuildTarget(
              id = label,
              tags = targetInfo.tagsList,
              dependencies = targetDependencies,
              kind = targetKind,
              sources = sources,
              resources = resources,
              baseDirectory = baseDirectory,
              noBuild = false,
              data = null,
            )
            project.targetUtils.addTargets(mapOf(label to rawBuildTarget), project)

            val moduleDeps = buildModuleDependencies(rawBuildTarget, project, snapshot, storage)
            dependencies.addAll(moduleDeps)
            result[label] = rawBuildTarget to dependencies
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      } catch (ex: Exception) {
        ex.printStackTrace()
      }
      return result
    }

    /**
     * Builds module dependencies from a RawBuildTarget, checking if dependencies exist
     * as regular modules or library modules.
     */
    private fun buildModuleDependencies(
      rawBuildTarget: RawBuildTarget,
      project: Project,
      snapshot: ImmutableEntityStorage,
      storage: MutableEntityStorage,
    ): List<ModuleDependency> {
      return rawBuildTarget.dependencies.map { dependencyLabel ->
        val baseDependencyName = dependencyLabel.label.formatAsModuleName(project)
        // First, check if a module with the base name exists in the snapshot
        val baseModuleId = ModuleId(baseDependencyName)
        val baseModuleExists = snapshot.resolve(baseModuleId) != null || storage.resolve(baseModuleId) != null
        val depModuleName = if (baseModuleExists) {
          // Module exists, use it
          baseDependencyName
        } else {
          // Module doesn't exist, check if a library module with prefix exists
          val libraryModuleName = LibraryGraph.addLibraryModulePrefix(baseDependencyName)
          val libraryModuleId = ModuleId(libraryModuleName)
          val libraryModuleExists = snapshot.resolve(libraryModuleId) != null || storage.resolve(libraryModuleId) != null
          if (libraryModuleExists) {
            libraryModuleName
          } else {
            baseDependencyName
          }
        }
        // Use interners to deduplicate instances
        moduleDependencyInterner.intern(
          ModuleDependency(
            module = moduleIdInterner.intern(ModuleId(depModuleName)),
            exported = false,
            scope = DependencyScope.COMPILE,
            productionOnTest = true
          )
        ) as ModuleDependency
      }
    }

    // TODO: these infer functions are copy pasted from other class that are under work from Jetbrains
    //  use the properly refactored public util functions instead when available
    // source: AspectClientProjectMapper.kt#L144
    private fun inferKind(
      tags: Set<Tag>,
      kindString: String,
      languages: Set<LanguageClass>,
    ): TargetKind {
      val ruleType =
        when {
          tags.contains(Tag.TEST) -> RuleType.TEST
          tags.contains(Tag.APPLICATION) -> RuleType.BINARY
          tags.contains(Tag.LIBRARY) -> RuleType.LIBRARY
          else -> RuleType.UNKNOWN
        }
      return TargetKind(
        kindString = kindString,
        languageClasses = languages,
        ruleType = ruleType,
      )
    }

    // source: AspectBazelProjectMapper.kt#L832
    private val languagesFromKinds: Map<String, Set<LanguageClass>> =
      mapOf(
        "java_library" to setOf(LanguageClass.JAVA),
        "java_binary" to setOf(LanguageClass.JAVA),
        "java_test" to setOf(LanguageClass.JAVA),
        // a workaround to register this target type as Java module in IntelliJ IDEA
        "intellij_plugin_debug_target" to setOf(LanguageClass.JAVA),
        "kt_jvm_library" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "kt_jvm_binary" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "kt_jvm_test" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "scala_library" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        "scala_binary" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        "scala_test" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        // rules_jvm from IntelliJ monorepo
        "jvm_library" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "jvm_binary" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "jvm_resources" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
        "go_binary" to setOf(LanguageClass.GO),
        "go_test" to setOf(LanguageClass.GO),
        "go_library" to setOf(LanguageClass.GO),
        "go_source" to setOf(LanguageClass.GO),
        "py_binary" to setOf(LanguageClass.PYTHON),
        "py_test" to setOf(LanguageClass.PYTHON),
        "py_library" to setOf(LanguageClass.PYTHON),
      )

    // source: AspectBazelProjectMapper #L858
    private fun inferLanguages(target: TargetInfo): Set<LanguageClass> =
      buildSet {
        if (target.hasJvmTargetInfo()) {
          add(LanguageClass.JAVA)
        }
        if (target.hasPythonTargetInfo()) {
          add(LanguageClass.PYTHON)
        }
        if (target.hasGoTargetInfo()) {
          add(LanguageClass.GO)
        }
        languagesFromKinds[target.kind]?.let {
          addAll(it)
        }
      }
  }
}
