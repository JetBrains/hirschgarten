package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

@ApiStatus.Internal
val JAVA_SOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-source")

@ApiStatus.Internal
val JAVA_TEST_SOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-test")

@ApiStatus.Internal
val JAVA_RESOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-resource")

@ApiStatus.Internal
val JAVA_TEST_RESOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-test-resource")

// RC: replaces `SourcesItemToJavaSourceRootTransformer` + `SourceItemToSourceRootTransformer` +
// `JavaSourceEntityUpdater` + the source-side of `ContentRootEntityUpdater`;
// the `JavaSourceRoot` / `ContentRoot` / `GenericSourceRoot` wrappers are dropped
@ApiStatus.Internal
object SourceRootBuilder {
  data class ResolvedSourceRoot(
    val sourcePath: Path,
    val generated: Boolean,
    val packagePrefix: String,
    val rootType: SourceRootTypeId,
  )

  fun resolve(
    target: RawBuildTarget,
    testSourcesGlob: ProjectViewGlobSet,
    packagePrefixes: JvmPackagePrefixCalculator,
    testTargets: Set<Label>,
  ): List<ResolvedSourceRoot> {
    val prefixes = packagePrefixes.get(target)
    fun Path.convert(generated: Boolean) =
      ResolvedSourceRoot(
        sourcePath = this,
        generated = generated,
        packagePrefix = prefixes[this] ?: "",
        rootType = when {
          target.kind.ruleType == RuleType.TEST -> JAVA_TEST_SOURCE_ROOT_TYPE
          target.id in testTargets -> JAVA_TEST_SOURCE_ROOT_TYPE
          testSourcesGlob.matches(this) -> JAVA_TEST_SOURCE_ROOT_TYPE
          else -> JAVA_SOURCE_ROOT_TYPE
        },
      )

    return target.sources.map { it.convert(generated = false) } +
           target.generatedSources.map { it.convert(generated = true) }
  }

  /**
   * writes a (ContentRootEntity, SourceRootEntity, JavaSourceRootPropertiesEntity) triple per source root.
   *
   * if [singleContentRoot] is true (the hirschgarten workspacemodel-module special case), a single content
   * root is created at the parent directory of the first source root and shared by all source roots.
   */
  fun write(
    sourceRoots: List<ResolvedSourceRoot>,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
    singleContentRoot: Boolean = false,
  ) {
    if (sourceRoots.isEmpty()) {
      return
    }
    val contentRoots = if (singleContentRoot) {
      val commonPath = sourceRoots.first().sourcePath.parent
      val single = addContentRoots(listOf(commonPath), parentModuleEntity, virtualFileUrlManager, storage).single()
      List(sourceRoots.size) { single }
    }
    else {
      addContentRoots(sourceRoots.map { it.sourcePath }, parentModuleEntity, virtualFileUrlManager, storage)
    }
    val sourceRootEntities = (contentRoots zip sourceRoots).map { (contentRoot, resolved) ->
      addSourceRootEntity(storage, contentRoot, resolved.sourcePath, resolved.rootType, virtualFileUrlManager)
    }
    for ((sourceRootEntity, resolved) in sourceRootEntities zip sourceRoots) {
      addJavaSourceRootPropertiesEntity(storage, sourceRootEntity, resolved.generated, resolved.packagePrefix)
    }
  }

  private fun addContentRoots(
    paths: List<Path>,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ): List<ContentRootEntity> {
    val entitySource = parentModuleEntity.entitySource
    val entities = paths.map { path ->
      ContentRootEntity(
        url = path.toResolvedVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
        entitySource = entitySource,
      )
    }
    val updated = storage.modifyModuleEntity(parentModuleEntity) {
      contentRoots += entities
    }
    return updated.contentRoots.takeLast(entities.size)
  }

  private fun addSourceRootEntity(
    storage: MutableEntityStorage,
    contentRoot: ContentRootEntity,
    sourcePath: Path,
    rootType: SourceRootTypeId,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): SourceRootEntity {
    val entity = SourceRootEntity(
      url = sourcePath.toVirtualFileUrl(virtualFileUrlManager),
      rootTypeId = rootType,
      entitySource = contentRoot.entitySource,
    )
    val updated = storage.modifyContentRootEntity(contentRoot) {
      sourceRoots += entity
    }
    return updated.sourceRoots.last()
  }

  private fun addJavaSourceRootPropertiesEntity(
    storage: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    generated: Boolean,
    packagePrefix: String,
  ) {
    val entity = JavaSourceRootPropertiesEntity(
      generated = generated,
      packagePrefix = packagePrefix,
      entitySource = sourceRoot.entitySource,
    )
    storage.modifySourceRootEntity(sourceRoot) {
      javaSourceRoots = listOf(entity)
    }
  }
}
