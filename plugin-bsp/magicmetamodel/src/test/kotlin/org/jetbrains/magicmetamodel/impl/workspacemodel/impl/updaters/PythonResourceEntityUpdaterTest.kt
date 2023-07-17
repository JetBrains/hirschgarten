package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentPythonModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("pythonResourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class PythonResourceEntityUpdaterTest : WorkspaceModelWithParentPythonModuleBaseTest() {
  private lateinit var pythonResourceEntityUpdater: PythonResourceEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)
    pythonResourceEntityUpdater = PythonResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one python resource root to the workspace model`() {
    // given
    val resourcePath = URI.create("file:///root/dir/example/resource/File.txt").toPath()
    val pythonResourceRoot = PythonResourceRoot(resourcePath)

    //when
    val returnedPythonResourceRootEntity = runTestWriteAction {
      pythonResourceEntityUpdater.addEntity(pythonResourceRoot, parentModuleEntity)
    }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)

    val expectedPythonResourceRootEntity = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl,
        rootType = "python-resource"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )


    returnedPythonResourceRootEntity shouldBeEqual expectedPythonResourceRootEntity
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
      expectedPythonResourceRootEntity
    )
  }

  @Test
  fun `shoulde add multiple python resource roots to the workspace model`() {
    // given
    val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
    val pythonResourceRoot1 = PythonResourceRoot(resourcePath1)

    val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
    val pythonResourceRoot2 = PythonResourceRoot(resourcePath2)

    val resourcePath3 = URI.create("file:///root/dir/example/resource/File3.txt").toPath()
    val pythonResourceRoot3 = PythonResourceRoot(resourcePath2)

    val pythonResourceRoots = listOf(pythonResourceRoot1, pythonResourceRoot2, pythonResourceRoot3)

    // when
    val returnedPythonResourceRootEntities = runTestWriteAction {
      pythonResourceEntityUpdater.addEntries(pythonResourceRoots, parentModuleEntity)
    }

    // then
    val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)

    val expectedPythonResourceRootEntity1 = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl1,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl1,
        rootType = "python-resource"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )

    val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)

    val expectedPythonResourceRootEntity2 = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl2,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl2,
        rootType = "python-resource"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )

    val virtualResourceUrl3 = resourcePath3.toVirtualFileUrl(virtualFileUrlManager)

    val expectedPythonResourceRootEntity3 = ExpectedSourceRootEntity(
      contentRootEntity = ContentRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl3,
        excludedPatterns = emptyList()
      ),
      sourceRootEntity = SourceRootEntity(
        entitySource = parentModuleEntity.entitySource,
        url = virtualResourceUrl3,
        rootType = "python-resource"
      ) {},
      parentModuleEntity = parentModuleEntity,
    )

    val expectedPythonResourceRootEntities = listOf(
      expectedPythonResourceRootEntity1,
      expectedPythonResourceRootEntity2,
      expectedPythonResourceRootEntity3
    )

    returnedPythonResourceRootEntities shouldContainExactlyInAnyOrder expectedPythonResourceRootEntities
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder expectedPythonResourceRootEntities
  }
}
