@file:Suppress("MaxLineLength")

package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("ContentRootEntityUpdater.addEntity()")
internal class ContentRootEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {
  private lateinit var contentRootEntityUpdater: ContentRootEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
    contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one content root to the workspace model with excluded files`() {
    // given
    val contentPath = Path("/root/dir/example/resource/File.txt")
    val excludedPath = Path("/root/dir/example/resource/File2.txt")
    val contentRoot =
      ContentRoot(
        path = contentPath,
        excluded = listOf(excludedPath),
      )

    // when
    runTestWriteAction {
      contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity =
      ExpectedContentRootEntity(
        url = contentPath.toVirtualFileUrl(virtualFileUrlManager),
        parentModuleEntity = parentModuleEntity,
        excludedUrls = listOf(
          ExcludeUrlEntity(
            url = excludedPath.toVirtualFileUrl(virtualFileUrlManager),
            entitySource = parentModuleEntity.entitySource,
          ),
        ),
      )

    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
  }

  @Test
  fun `should add one content root to the workspace model`() {
    // given
    val contentPath = Path("/root/dir/example/resource/File.txt")
    val contentRoot =
      ContentRoot(
        path = contentPath,
      )

    // when
    runTestWriteAction {
      contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity =
      ExpectedContentRootEntity(
        url = contentPath.toVirtualFileUrl(virtualFileUrlManager),
        parentModuleEntity = parentModuleEntity,
      )

    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
  }

  @Test
  fun `should add multiple content root to the workspace model`() {
    // given
    val contentPath1 = Path("/root/dir/example/resource/File1.txt")
    val contentRoot1 =
      ContentRoot(
        path = contentPath1,
      )

    val contentPath2 = Path("/root/dir/example/resource/File2.txt")
    val contentRoot2 =
      ContentRoot(
        path = contentPath2,
      )

    val contentPath3 = Path("/root/dir/another/example/resource/File3.txt")
    val contentRoot3 =
      ContentRoot(
        path = contentPath3,
      )

    val contentRoots = listOf(contentRoot1, contentRoot2, contentRoot3)

    // when
    runTestWriteAction {
      contentRootEntityUpdater.addEntities(contentRoots, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity1 =
      ExpectedContentRootEntity(
        url = contentPath1.toVirtualFileUrl(virtualFileUrlManager),
        parentModuleEntity = parentModuleEntity,
      )

    val expectedContentRootEntity2 =
      ExpectedContentRootEntity(
        url = contentPath2.toVirtualFileUrl(virtualFileUrlManager),
        parentModuleEntity = parentModuleEntity,
      )

    val expectedContentRootEntity3 =
      ExpectedContentRootEntity(
        url = contentPath3.toVirtualFileUrl(virtualFileUrlManager),
        parentModuleEntity = parentModuleEntity,
      )

    val expectedContentRootEntries =
      listOf(expectedContentRootEntity1, expectedContentRootEntity2, expectedContentRootEntity3)

    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder expectedContentRootEntries
  }
}
