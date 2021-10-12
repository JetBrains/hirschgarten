package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

private data class ExpectedContentRootEntityDetails(
  val contentRootEntity: ContentRootEntity,
)

@DisplayName("ContentRootEntityUpdater.addEntity()")
internal class ContentRootEntityUpdaterTest : WorkspaceModelEntityWithParentModuleUpdaterBaseTest() {

  @Test
  fun `should add one content root to the workspace model`() {
    // given
    val contentPath = URI.create("file:///root/dir/example/resource/File.txt").toPath()
    val contentRoot = ContentRoot(contentPath)

    // when
    val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedContentRootEntity: ContentRootEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedContentRootEntity = contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
    }

    // then
    val virtualContentUrl = contentPath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntityDetails = ExpectedContentRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualContentUrl, emptyList(), emptyList()),
    )

    validateContentRootEntity(returnedContentRootEntity, expectedContentRootEntityDetails)

    workspaceModelLoadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedContentRootEntityDetails), this::validateContentRootEntity
    )
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
    val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedContentRootEntries: Collection<ContentRootEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedContentRootEntries = contentRootEntityUpdater.addEntries(contentRoots, parentModuleEntity)
    }

    // then
    val virtualContentUrl1 = contentPath1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntityDetails1 = ExpectedContentRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualContentUrl1, emptyList(), emptyList()),
    )

    val virtualContentUrl2 = contentPath2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntityDetails2 = ExpectedContentRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualContentUrl2, emptyList(), emptyList()),
    )

    val virtualContentUrl3 = contentPath3.toVirtualFileUrl(virtualFileUrlManager)
    val expectedContentRootEntityDetails3 = ExpectedContentRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualContentUrl3, emptyList(), emptyList()),
    )

    returnedContentRootEntries shouldContainExactlyInAnyOrder Pair(
      listOf(expectedContentRootEntityDetails1, expectedContentRootEntityDetails2, expectedContentRootEntityDetails3),
      this::validateContentRootEntity
    )

    workspaceModelLoadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedContentRootEntityDetails1, expectedContentRootEntityDetails2, expectedContentRootEntityDetails3),
      this::validateContentRootEntity
    )
  }

  private fun validateContentRootEntity(
    actual: ContentRootEntity,
    expected: ExpectedContentRootEntityDetails
  ) {
    actual.url shouldBe expected.contentRootEntity.url
    actual.excludedUrls shouldBe expected.contentRootEntity.excludedUrls
    actual.excludedPatterns shouldBe expected.contentRootEntity.excludedPatterns

    val actualModuleEntity = actual.module
    actualModuleEntity shouldBe parentModuleEntity
  }
}
