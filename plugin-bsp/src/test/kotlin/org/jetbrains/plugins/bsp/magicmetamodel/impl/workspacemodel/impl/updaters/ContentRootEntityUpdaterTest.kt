@file:Suppress("MaxLineLength")

package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
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
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
    contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one content root to the workspace model`() {
    // given
    val contentPath = Path("/root/dir/example/resource/File.txt")
    val contentRoot = ContentRoot(
      path = contentPath,
      excludedPaths = listOf(Path("/root/dir/example/resource/ExcludedFile.txt")),
    )

    // when
    runTestWriteAction {
      contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity = ExpectedContentRootEntity(
      url = contentPath.toVirtualFileUrl(virtualFileUrlManager),
      excludedUrls = listOf(Path("/root/dir/example/resource/ExcludedFile.txt").toVirtualFileUrl(virtualFileUrlManager)),
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
    )

    // TODO it's odd, but it doubles the excluded urls xd
    // returnedContentRootEntity shouldBeEqual expectedContentRootEntity
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
  }

  @Test
  fun `should add multiple content root to the workspace model`() {
    // given
    val contentPath1 = Path("/root/dir/example/resource/File1.txt")
    val contentRoot1 = ContentRoot(
      path = contentPath1,
      excludedPaths = listOf(Path("/root/dir/example/resource/ExcludedFile.txt")),
    )

    val contentPath2 = Path("/root/dir/example/resource/File2.txt")
    val contentRoot2 = ContentRoot(
      path = contentPath2,
    )

    val contentPath3 = Path("/root/dir/another/example/resource/File3.txt")
    val contentRoot3 = ContentRoot(
      path = contentPath3,
    )

    val contentRoots = listOf(contentRoot1, contentRoot2, contentRoot3)

    // when
    runTestWriteAction {
      contentRootEntityUpdater.addEntries(contentRoots, parentModuleEntity)
    }

    // then
    val expectedContentRootEntity1 = ExpectedContentRootEntity(
      url = contentPath1.toVirtualFileUrl(virtualFileUrlManager),
      excludedPatterns = emptyList(),
      excludedUrls = listOf(
        Path("/root/dir/example/resource/ExcludedFile.txt").toVirtualFileUrl(virtualFileUrlManager),
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntity2 = ExpectedContentRootEntity(
      url = contentPath2.toVirtualFileUrl(virtualFileUrlManager),
      excludedUrls = emptyList(),
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntity3 = ExpectedContentRootEntity(
      url = contentPath3.toVirtualFileUrl(virtualFileUrlManager),
      excludedUrls = emptyList(),
      excludedPatterns = emptyList(),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntries =
      listOf(expectedContentRootEntity1, expectedContentRootEntity2, expectedContentRootEntity3)

    // TODO it's odd, but it doubles the excluded urls xd
    // returnedContentRootEntries shouldContainExactlyInAnyOrder expectedContentRootEntries
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder expectedContentRootEntries
  }
}
