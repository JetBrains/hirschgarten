package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("ContentRootEntityUpdater.addEntity()")
internal class ContentRootEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {

  private lateinit var contentRootEntityUpdater: ContentRootEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceModel, virtualFileUrlManager, projectConfigSource)
    contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one content root to the workspace model`() {
    // given
    val contentPath = URI.create("file:///root/dir/example/resource/File.txt").toPath()
    val contentRoot = ContentRoot(contentPath)

    // when
    val returnedContentRootEntity = runTestWriteAction {
      contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val virtualContentUrl = contentPath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(virtualContentUrl, emptyList(), emptyList()),
      parentModuleEntity = parentModuleEntity,
    )

    returnedContentRootEntity shouldBeEqual expectedContentRootEntity
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
  }

  @Test
  fun `should add multiple content root to the workspace model`() {
    // given
    val contentPath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
    val contentRoot1 = ContentRoot(contentPath1)

    val contentPath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
    val contentRoot2 = ContentRoot(contentPath2)

    val contentPath3 = URI.create("file:///root/dir/another/example/resource/File3.txt").toPath()
    val contentRoot3 = ContentRoot(contentPath3)

    val contentRoots = listOf(contentRoot1, contentRoot2, contentRoot3)

    // when
    val returnedContentRootEntries = runTestWriteAction {
      contentRootEntityUpdater.addEntries(contentRoots, parentModuleEntity)
    }

    // then
    val virtualContentUrl1 = contentPath1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity1 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(virtualContentUrl1, emptyList(), emptyList()),
      parentModuleEntity = parentModuleEntity,
    )

    val virtualContentUrl2 = contentPath2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity2 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(virtualContentUrl2, emptyList(), emptyList()),
      parentModuleEntity = parentModuleEntity,
    )

    val virtualContentUrl3 = contentPath3.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntity3 = ExpectedContentRootEntity(
      contentRootEntity = ContentRootEntity(virtualContentUrl3, emptyList(), emptyList()),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedContentRootEntries =
      listOf(expectedContentRootEntity1, expectedContentRootEntity2, expectedContentRootEntity3)

    returnedContentRootEntries shouldContainExactlyInAnyOrder expectedContentRootEntries
    loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder expectedContentRootEntries
  }
}
