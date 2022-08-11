package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaResourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("javaResourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class JavaResourceEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {

  private lateinit var javaResourceEntityUpdater: JavaResourceEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager)
    javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one java resource root to the workspace model`() {
    // given
    val resourcePath = URI.create("file:///root/dir/example/resource/File.txt").toPath()
    val javaResourceRoot = JavaResourceRoot(resourcePath)

    // when
    val returnedJavaResourceRootEntity = runTestWriteAction {
      javaResourceEntityUpdater.addEntity(javaResourceRoot, parentModuleEntity)
    }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity = ExpectedJavaResourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl,
        rootType = "java-resource"
      ),
      javaResourceRootEntity = JavaResourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = false,
        relativeOutputPath = ""
      ),
      parentModuleEntity = parentModuleEntity,
    )

    returnedJavaResourceRootEntity shouldBeEqual expectedJavaResourceRootEntity
    loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
      expectedJavaResourceRootEntity
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
    val returnedJavaResourceRootEntries = runTestWriteAction {
      javaResourceEntityUpdater.addEntries(javaResourceRoots, parentModuleEntity)
    }

    // then
    val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity1 = ExpectedJavaResourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl1,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl1,
        rootType = "java-resource"
      ),
      javaResourceRootEntity = JavaResourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = false,
        relativeOutputPath = ""
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity2 = ExpectedJavaResourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl2,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl2,
        rootType = "java-resource"
      ),
      javaResourceRootEntity = JavaResourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = false,
        relativeOutputPath = ""
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val virtualResourceUrl3 = resourcePath3.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity3 = ExpectedJavaResourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl3,
        excludedUrls = emptyList(),
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl3,
        rootType = "java-resource"
      ),
      javaResourceRootEntity = JavaResourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        generated = false,
        relativeOutputPath = ""
      ),
      parentModuleEntity = parentModuleEntity,
    )

    val expectedJavaResourceRootEntries = listOf(
      expectedJavaResourceRootEntity1,
      expectedJavaResourceRootEntity2,
      expectedJavaResourceRootEntity3
    )

    returnedJavaResourceRootEntries shouldContainExactlyInAnyOrder expectedJavaResourceRootEntries
    loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder expectedJavaResourceRootEntries
  }
}
