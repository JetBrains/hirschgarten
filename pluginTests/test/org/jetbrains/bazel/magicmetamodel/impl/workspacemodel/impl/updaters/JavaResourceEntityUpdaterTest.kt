package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("javaResourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
class JavaResourceEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {
  private lateinit var javaResourceEntityUpdater: JavaResourceEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
    javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should handle excluded when adding java resource root to the workspace model `() {
    // given
    val resourcePath = Path("/root/dir/example/resource/File.txt")
    val excludedPath = Path("/root/dir/example/resource/ExcludedFile.txt")
    val javaResourceRoot = ResourceRoot(resourcePath, SourceRootTypeId("java-resource"), excluded = listOf(excludedPath))

    // when
    val returnedJavaResourceRootEntity =
      runTestWriteAction {
        javaResourceEntityUpdater.addEntity(javaResourceRoot, parentModuleEntity)
      }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl,
            excludedPatterns = emptyList(),
          ).apply {
            excludedUrls = listOf(
              ExcludeUrlEntity(
                url = excludedPath.toVirtualFileUrl(virtualFileUrlManager),
                entitySource = parentModuleEntity.entitySource,
              ),
            )
          },
        sourceRootEntity = SourceRootEntity(
          entitySource = parentModuleEntity.entitySource,
          url = virtualResourceUrl,
          rootTypeId = SourceRootTypeId("java-resource"),
        ) {
          javaResourceRoots =
            listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = parentModuleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
        },
        parentModuleEntity = parentModuleEntity,
      )

    returnedJavaResourceRootEntity.sourceRoot shouldBeEqual expectedJavaResourceRootEntity
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder
      listOf(
        expectedJavaResourceRootEntity,
      )
  }

  @Test
  fun `should add one java resource root to the workspace model`() {
    // given
    val resourcePath = Path("/root/dir/example/resource/File.txt")
    val javaResourceRoot = ResourceRoot(resourcePath, SourceRootTypeId("java-resource"))

    // when
    val returnedJavaResourceRootEntity =
      runTestWriteAction {
        javaResourceEntityUpdater.addEntity(javaResourceRoot, parentModuleEntity)
      }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl,
            excludedPatterns = emptyList(),
          ),
        sourceRootEntity =
          SourceRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl,
            rootTypeId = SourceRootTypeId("java-resource"),
          ) {
            javaResourceRoots =
              listOf(
                JavaResourceRootPropertiesEntity(
                  entitySource = parentModuleEntity.entitySource,
                  generated = false,
                  relativeOutputPath = "",
                ),
              )
          },
        parentModuleEntity = parentModuleEntity,
      )

    returnedJavaResourceRootEntity.sourceRoot shouldBeEqual expectedJavaResourceRootEntity
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder
      listOf(
        expectedJavaResourceRootEntity,
      )
  }

  @Test
  fun `should add one java test resource root to the workspace model`() {
    // given
    val resourcePath = Path("/root/dir/example/resource/File.txt")
    val javaResourceRoot = ResourceRoot(resourcePath, SourceRootTypeId("java-test-resource"))

    // when
    val returnedJavaResourceRootEntity =
      runTestWriteAction {
        javaResourceEntityUpdater.addEntity(javaResourceRoot, parentModuleEntity)
      }

    // then
    val virtualResourceUrl = resourcePath.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl,
            excludedPatterns = emptyList(),
          ),
        sourceRootEntity =
          SourceRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl,
            rootTypeId = SourceRootTypeId("java-test-resource"),
          ) {
            javaResourceRoots =
              listOf(
                JavaResourceRootPropertiesEntity(
                  entitySource = parentModuleEntity.entitySource,
                  generated = false,
                  relativeOutputPath = "",
                ),
              )
          },
        parentModuleEntity = parentModuleEntity,
      )

    returnedJavaResourceRootEntity.sourceRoot shouldBeEqual expectedJavaResourceRootEntity
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder
      listOf(
        expectedJavaResourceRootEntity,
      )
  }

  @Test
  fun `should add multiple java resource roots to the workspace model`() {
    // given
    val resourcePath1 = Path("/root/dir/example/resource/File1.txt")
    val javaResourceRoot1 = ResourceRoot(resourcePath1, SourceRootTypeId("java-resource"))

    val resourcePath2 = Path("/root/dir/example/resource/File2.txt")
    val javaResourceRoot2 = ResourceRoot(resourcePath2, SourceRootTypeId("java-resource"))

    val resourcePath3 = Path("/root/dir/example/another/resource/File3.txt")
    val javaResourceRoot3 = ResourceRoot(resourcePath3, SourceRootTypeId("java-resource"))

    val javaResourceRoots = listOf(javaResourceRoot1, javaResourceRoot2, javaResourceRoot3)

    // when
    val returnedJavaResourceRootEntries =
      runTestWriteAction {
        javaResourceEntityUpdater.addEntities(javaResourceRoots, parentModuleEntity)
      }

    // then
    val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity1 =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl1,
            excludedPatterns = emptyList(),
          ),
        sourceRootEntity =
          SourceRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl1,
            rootTypeId = SourceRootTypeId("java-resource"),
          ) {
            this.javaResourceRoots =
              listOf(
                JavaResourceRootPropertiesEntity(
                  entitySource = parentModuleEntity.entitySource,
                  generated = false,
                  relativeOutputPath = "",
                ),
              )
          },
        parentModuleEntity = parentModuleEntity,
      )

    val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity2 =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl2,
            excludedPatterns = emptyList(),
          ),
        sourceRootEntity =
          SourceRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl2,
            rootTypeId = SourceRootTypeId("java-resource"),
          ) {
            this.javaResourceRoots =
              listOf(
                JavaResourceRootPropertiesEntity(
                  entitySource = parentModuleEntity.entitySource,
                  generated = false,
                  relativeOutputPath = "",
                ),
              )
          },
        parentModuleEntity = parentModuleEntity,
      )

    val virtualResourceUrl3 = resourcePath3.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaResourceRootEntity3 =
      ExpectedSourceRootEntity(
        contentRootEntity =
          ContentRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl3,
            excludedPatterns = emptyList(),
          ),
        sourceRootEntity =
          SourceRootEntity(
            entitySource = parentModuleEntity.entitySource,
            url = virtualResourceUrl3,
            rootTypeId = SourceRootTypeId("java-resource"),
          ) {
            this.javaResourceRoots =
              listOf(
                JavaResourceRootPropertiesEntity(
                  entitySource = parentModuleEntity.entitySource,
                  generated = false,
                  relativeOutputPath = "",
                ),
              )
          },
        parentModuleEntity = parentModuleEntity,
      )

    val expectedJavaResourceRootEntries =
      listOf(
        expectedJavaResourceRootEntity1,
        expectedJavaResourceRootEntity2,
        expectedJavaResourceRootEntity3,
      )

    returnedJavaResourceRootEntries.map { it.sourceRoot } shouldContainExactlyInAnyOrder expectedJavaResourceRootEntries
    loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder expectedJavaResourceRootEntries
  }
}
