package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

private data class ExpectedJavaResourceRootEntityDetails(
  val contentRootEntity: ContentRootEntity,
  val sourceRootEntity: SourceRootEntity,
  val javaResourceRootEntity: JavaResourceRootEntity,
)

@DisplayName("javaResourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class JavaResourceEntityUpdaterTest : WorkspaceModelEntityWithParentModuleUpdaterBaseTest() {

  @Test
  fun `should add one java resource root to the workspace model`() {
    // given
    val resourcePath = URI.create("file:///root/dir/example/resource/File.txt").toPath()
    val javaResourceRoot = JavaResourceRoot(resourcePath)

    // when
    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedJavaResourceRootEntity: JavaResourceRootEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedJavaResourceRootEntity = javaResourceEntityUpdater.addEntity(javaResourceRoot, parentModuleEntity)
    }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntityDetails = ExpectedJavaResourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualResourceUrl, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualResourceUrl, "java-resource"),
      javaResourceRootEntity = JavaResourceRootEntity(false, ""),
    )

    validateJavaResourceRootEntity(returnedJavaResourceRootEntity, expectedJavaResourceRootEntityDetails)

    workspaceModelLoadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedJavaResourceRootEntityDetails), this::validateJavaResourceRootEntity
    )
  }

  @Test
  fun `should add multiple java resource roots to the workspace model`() {
    // given
    val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
    val javaResourceRoot1 = JavaResourceRoot(resourcePath1)

    val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
    val javaResourceRoot2 = JavaResourceRoot(resourcePath2)

    val resourcePath3 = URI.create("file:///root/dir/example/another/resource/File3.txt").toPath()
    val javaResourceRoot3 = JavaResourceRoot(resourcePath3)

    val javaResourceRoots = listOf(javaResourceRoot1, javaResourceRoot2, javaResourceRoot3)

    // when
    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedJavaResourceRootEntries: Collection<JavaResourceRootEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedJavaResourceRootEntries = javaResourceEntityUpdater.addEntries(javaResourceRoots, parentModuleEntity)
    }

    // then
    val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntityDetails1 = ExpectedJavaResourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualResourceUrl1, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualResourceUrl1, "java-resource"),
      javaResourceRootEntity = JavaResourceRootEntity(false, ""),
    )

    val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntityDetails2 = ExpectedJavaResourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualResourceUrl2, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualResourceUrl2, "java-resource"),
      javaResourceRootEntity = JavaResourceRootEntity(false, ""),
    )

    val virtualResourceUrl3 = resourcePath3.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntityDetails3 = ExpectedJavaResourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualResourceUrl3, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualResourceUrl3, "java-resource"),
      javaResourceRootEntity = JavaResourceRootEntity(false, ""),
    )

    returnedJavaResourceRootEntries shouldContainExactlyInAnyOrder Pair(
      listOf(
        expectedJavaResourceRootEntityDetails1,
        expectedJavaResourceRootEntityDetails2,
        expectedJavaResourceRootEntityDetails3
      ),
      this::validateJavaResourceRootEntity
    )

    workspaceModelLoadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(
        expectedJavaResourceRootEntityDetails1,
        expectedJavaResourceRootEntityDetails2,
        expectedJavaResourceRootEntityDetails3
      ),
      this::validateJavaResourceRootEntity
    )
  }

  private fun validateJavaResourceRootEntity(
    actual: JavaResourceRootEntity,
    expected: ExpectedJavaResourceRootEntityDetails
  ) {
    actual.generated shouldBe expected.javaResourceRootEntity.generated
    actual.relativeOutputPath shouldBe expected.javaResourceRootEntity.relativeOutputPath

    val actualSourceRoot = actual.sourceRoot
    actualSourceRoot.url shouldBe expected.sourceRootEntity.url
    actualSourceRoot.rootType shouldBe expected.sourceRootEntity.rootType

    val actualContentRoot = actualSourceRoot.contentRoot
    actualContentRoot.url shouldBe expected.contentRootEntity.url
    actualContentRoot.excludedUrls shouldBe expected.contentRootEntity.excludedUrls
    actualContentRoot.excludedPatterns shouldBe expected.contentRootEntity.excludedPatterns

    val actualModuleEntity = actualContentRoot.module
    actualModuleEntity shouldBe parentModuleEntity
  }
}
