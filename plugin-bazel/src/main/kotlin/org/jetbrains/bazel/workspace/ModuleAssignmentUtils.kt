/**
 * Shared utilities for dynamically adding source files to workspace model modules.
 * Used by [BazelFileEventListener][org.jetbrains.bazel.workspace.fileEvents.BazelFileEventListener]
 * for VFS events and [AddFileToModuleAction][org.jetbrains.bazel.action.registered.AddFileToModuleAction]
 * for manual user actions.
 */
@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.TaskGroupId

private val logger = Logger.getInstance("org.jetbrains.bazel.workspace.ModuleAssignment")

/**
 * Adds a single source file to a module's workspace model as a content root with a source root.
 * Also attaches [JavaSourceRootPropertiesEntity] with the correct package prefix for JVM files,
 * which is required for code intelligence (highlighting, import resolution) to work.
 */
fun VirtualFileUrl.addToModule(
  entityStorageDiff: MutableEntityStorage,
  module: ModuleEntity,
  extension: String?,
  isTestModule: Boolean = false,
  packagePrefix: String = "",
) {
  if (module.contentRoots.any { it.url == this }) return // we don't want to duplicate content roots

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1917
  val sourceRootType =
    when (extension) {
      "java" -> if (isTestModule) SourceRootTypeId("java-test") else SourceRootTypeId("java-source")
      "kt" -> SourceRootTypeId("kotlin-source") // Kotlin uses same type for test and production
      "py" -> SourceRootTypeId("python-source") // Python uses same type for test and production
      else -> {
        logger.warn("Bazel recognised a file as a source, but we failed to parse its extension: .$extension")
        SourceRootTypeId("unknown-source")
      }
    }

  val sourceRoot =
    SourceRootEntity(
      url = this,
      entitySource = module.entitySource,
      rootTypeId = sourceRootType,
    )

  val contentRootEntity =
    ContentRootEntity(
      url = this,
      excludedPatterns = emptyList(),
      entitySource = module.entitySource,
    ) {
      sourceRoots += listOf(sourceRoot)
    }

  entityStorageDiff.modifyModuleEntity(module) { contentRoots += contentRootEntity }

  // Attach JavaSourceRootPropertiesEntity so IntelliJ knows the package prefix for this file.
  // Without this, SingleFileSourcesTracker cannot resolve the package and code intelligence
  // (highlighting, import resolution) won't work until a full re-sync.
  if (extension == "java" || extension == "kt") {
    val addedSourceRoot = entityStorageDiff.entities(SourceRootEntity::class.java).last { it.url == this }
    entityStorageDiff.modifySourceRootEntity(addedSourceRoot) {
      this.javaSourceRoots = listOf(
        JavaSourceRootPropertiesEntity(
          generated = false,
          packagePrefix = packagePrefix,
          entitySource = module.entitySource,
        )
      )
    }
  }
}

/**
 * Resolves the package prefix for a file being added to a module by looking up the
 * parent directory's [PackageMarkerEntity].
 */
fun resolvePackagePrefix(parentUrl: VirtualFileUrl?, module: ModuleEntity): String =
  parentUrl?.let { pUrl ->
    module.packageMarkerEntities.firstOrNull { it.root == pUrl }?.packagePrefix
  } ?: ""

suspend fun getModulesForFile(newFile: VirtualFile, project: Project): Set<Module> =
  readAction { ProjectFileIndex.getInstance(project).getModulesForFile(newFile, true) }

public suspend fun askForInverseSources(project: Project, fileUrl: VirtualFileUrl): InverseSourcesResult =
  project.connection.runWithServer { bspServer ->
    bspServer
      .buildTargetInverseSources(InverseSourcesParams(TaskGroupId.EMPTY.task("inverse-sources"), listOf(fileUrl.toPath())))
  }

/**
 * Converts a Label to a ModuleEntity, creating it via partial sync ([UnsyncedTargetUpdater]) if it doesn't exist.
 * Returns the module entity paired with whether it's a test module.
 */
suspend fun Label.toModuleEntity(snapshot: ImmutableEntityStorage, storageUpdates: MutableEntityStorage, project: Project): Pair<ModuleEntity, Boolean>? {
  val moduleId = ModuleId(this.formatAsModuleName(project))

  // First check if module exists in the mutable storage (from previous calls in this batch)
  // note in short, the snapshot + the mutable storage = the current state
  // the storage updates here contains already commited changes that are not pushed into the final module storage
  val existingInStorage = storageUpdates.resolve(moduleId) ?: snapshot.resolve(moduleId)
  var cachedTarget = project.targetUtils.getBuildTargetForLabel(this)
  if (existingInStorage != null) {
    // For existing modules, check if it's a test module from the cached target
    val isTestModule = (cachedTarget?.kind?.ruleType == RuleType.TEST)
    return existingInStorage to isTestModule
  }

  // Try to get build target information from TargetUtils first (for synced targets)
  val dependencies = mutableListOf<ModuleDependencyItem>()

  // Determine module type based on target kind (TEST or JAVA_MODULE for non-test)
  // If target is not in cache, trigger a partial sync to fetch it via UnsyncedTargetUpdater
  if (cachedTarget == null) {
    val result = UnsyncedTargetUpdater.fetchAndCacheUnsyncedTarget(this, project, snapshot, storageUpdates) ?: return null
    cachedTarget = result.first
    dependencies.addAll(result.second)
  }
  val isTestModule = cachedTarget.kind.ruleType == RuleType.TEST
  // Use BazelModuleEntitySource for dynamically created modules
  // Note: We can't use the full JPS entity source logic from ModuleEntityUpdater here because
  // BazelProjectModelExternalSource is not accessible from this package due to module boundaries.
  // Dynamically created modules (added via file listener) should use BazelModuleEntitySource.
  val entitySource = BazelModuleEntitySource(moduleId.name)
  val moduleEntity = ModuleEntity(
    name = moduleId.name,
    dependencies = dependencies,
    entitySource = entitySource,
  )
  val addedEntity = storageUpdates.addEntity(moduleEntity)
  return addedEntity to isTestModule
}
