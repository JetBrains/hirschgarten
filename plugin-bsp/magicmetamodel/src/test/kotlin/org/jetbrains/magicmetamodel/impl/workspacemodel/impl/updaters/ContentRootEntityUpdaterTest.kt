@file:Suppress("MaxLineLength")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ExcludeUrlEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
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
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager)
    contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one content root to the workspace model`() {
    // given
    val contentPath = Path("/root/dir/example/resource/File.txt")
    val contentRoot = ContentRoot(
      url = contentPath,
      excludedUrls = listOf(Path("/root/dir/example/resource/ExcludedFile.txt"))
    )

    // when
    val returnedContentRootEntity = runTestWriteAction {
      contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = contentPath.toVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
      ) {
        excludedUrls = listOf(
          ExcludeUrlEntity(
            Path("/root/dir/example/resource/ExcludedFile.txt").toVirtualFileUrl(virtualFileUrlManager),
            parentModuleEntity.entitySource
          )
        )
      },
      parentModuleEntity = parentModuleEntity,
    )

    returnedContentRootEntity shouldBeEqual expectedContentRootEntity
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
  }

  @Test
  fun `should add multiple content root to the workspace model`() {
    // given
    val contentPath1 = Path("/root/dir/example/resource/File1.txt")
    val contentRoot1 = ContentRoot(
      url = contentPath1,
      excludedUrls = listOf(Path("/root/dir/example/resource/ExcludedFile.txt"))
    )

    val contentPath2 = Path("/root/dir/example/resource/File2.txt")
    val contentRoot2 = ContentRoot(
      url = contentPath2
    )

    val contentPath3 = Path("/root/dir/another/example/resource/File3.txt")
    val contentRoot3 = ContentRoot(
      url = contentPath3
    )

    val contentRoots = listOf(contentRoot1, contentRoot2, contentRoot3)

    // when
    val returnedContentRootEntries = runTestWriteAction {
      contentRootEntityUpdater.addEntries(contentRoots, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity1 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = contentPath1.toVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
      ) {
        excludedUrls = listOf(
          ExcludeUrlEntity(
            Path("/root/dir/example/resource/ExcludedFile.txt").toVirtualFileUrl(virtualFileUrlManager),
            parentModuleEntity.entitySource
          )
        )
      },
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntity2 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = contentPath2.toVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntity3 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = contentPath3.toVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntries =
      listOf(expectedContentRootEntity1, expectedContentRootEntity2, expectedContentRootEntity3)

    returnedContentRootEntries shouldContainExactlyInAnyOrder expectedContentRootEntries
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder expectedContentRootEntries
  }
}
